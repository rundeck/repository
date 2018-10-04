/*
 * Copyright 2018 Rundeck, Inc. (http://rundeck.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.rundeck.repository.client.repository

import com.dtolabs.rundeck.core.storage.StorageTree
import com.rundeck.repository.ResponseBatch
import com.rundeck.repository.ResponseCodes
import com.rundeck.repository.ResponseMessage
import com.rundeck.repository.artifact.RepositoryArtifact
import com.rundeck.repository.client.artifact.RundeckRepositoryArtifact
import com.rundeck.repository.client.manifest.MemoryManifestService
import com.rundeck.repository.client.manifest.MemoryManifestSource
import com.rundeck.repository.client.manifest.StorageTreeManifestCreator
import com.rundeck.repository.client.manifest.StorageTreeManifestSource
import com.rundeck.repository.client.util.ArtifactFileset
import com.rundeck.repository.client.util.ArtifactUtils
import com.rundeck.repository.client.util.ResourceFactory
import com.rundeck.repository.client.validators.BinaryValidator
import com.rundeck.repository.definition.RepositoryDefinition
import com.rundeck.repository.api.ArtifactRepository
import com.rundeck.repository.events.RepositoryEventEmitter
import com.rundeck.repository.events.RepositoryUpdateEvent
import com.rundeck.repository.manifest.ManifestEntry
import com.rundeck.repository.manifest.ManifestService
import com.rundeck.repository.manifest.ManifestSource
import groovy.transform.PackageScope
import org.rundeck.storage.data.DataUtil

class StorageTreeArtifactRepository implements ArtifactRepository {
    private static final String MEMORY_MANIFEST_SOURCE = "memory"
    static final String ARTIFACT_BASE = "artifacts/"
    static final String BINARY_BASE = "binary/"
    @PackageScope
    StorageTree storageTree
    RepositoryDefinition repositoryDefinition
    private final ManifestService manifestService
    private final RepositoryEventEmitter eventEmitter
    private StorageTreeManifestCreator manifestCreator
    static final ResourceFactory resourceFactory = new ResourceFactory()
    private ManifestSource manifestSource

    StorageTreeArtifactRepository(StorageTree storageTree, RepositoryDefinition repositoryDefinition, RepositoryEventEmitter eventEmitter) {
        this(storageTree,repositoryDefinition)
        this.eventEmitter = eventEmitter

    }

    StorageTreeArtifactRepository(StorageTree storageTree, RepositoryDefinition repositoryDefinition) {
        if(!storageTree) throw new Exception("Unable to initialize storage tree repository. No storage tree provided.")
        if(!repositoryDefinition.configProperties.manifestPath) throw new Exception("Path to manifest in storage tree must be provided by setting configProperties.manifestPath in repository definition.")
        this.storageTree = storageTree
        this.repositoryDefinition = repositoryDefinition
        this.manifestSource = repositoryDefinition.configProperties.manifestType == MEMORY_MANIFEST_SOURCE ? new MemoryManifestSource() : new StorageTreeManifestSource(storageTree, this.repositoryDefinition.configProperties.manifestPath)
        this.manifestService = new MemoryManifestService(manifestSource)
        manifestCreator = new StorageTreeManifestCreator(storageTree)
    }

    @Override
    RepositoryDefinition getRepositoryDefinition() {
        return repositoryDefinition
    }

    @Override
    RepositoryArtifact getArtifact(final String artifactId, final String version = null) {
        String artifactVer = version ?: manifestService.getEntry(artifactId).currentVersion
        ArtifactUtils.createArtifactFromYamlStream(storageTree.getResource(ARTIFACT_BASE+ ArtifactUtils.artifactMetaFileName(artifactId, artifactVer)).contents.inputStream)
    }

    @Override
    InputStream getArtifactBinary(final String artifactId, final String version = null) {
        ManifestEntry entry = manifestService.getEntry(artifactId)
        String artifactVer = version ?: entry.currentVersion
        String extension = ArtifactUtils.artifactTypeFromNice(entry.artifactType).extension
        return storageTree.getResource(BINARY_BASE+ArtifactUtils.artifactBinaryFileName(artifactId, artifactVer, extension)).contents.inputStream
    }

    @Override
    ResponseBatch uploadArtifact(final InputStream artifactInputStream) {
        ResponseBatch responseBatch = new ResponseBatch()
        ArtifactFileset artifactFileset = ArtifactUtils.constructArtifactFileset(artifactInputStream)
        responseBatch.messages.addAll(BinaryValidator.validate(artifactFileset.artifact.artifactType, artifactFileset.artifactBinary).messages)
        if(!responseBatch.batchSucceeded()) return responseBatch

        responseBatch.messages.addAll(saveNewArtifact(artifactFileset.artifact).messages)
        responseBatch.messages.addAll(uploadArtifactBinary(artifactFileset.artifact, artifactFileset.artifactBinary.newInputStream()).messages)
        responseBatch
    }

    @Override
    ResponseBatch saveNewArtifact(final RepositoryArtifact artifact) {
        ResponseBatch response = new ResponseBatch()
        String artifactPath = ARTIFACT_BASE + artifact.getArtifactMetaFileName()
        Map meta = [:]
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream()
            ArtifactUtils.saveArtifactToOutputStream(artifact,baos)
            def resource = DataUtil.withStream(new ByteArrayInputStream(baos.toByteArray()), meta, resourceFactory)
            storageTree.createResource(artifactPath, resource)

            response.messages.add(new ResponseMessage(code: ResponseCodes.SUCCESS))
            //recreate manifest
            recreateAndSaveManifest()
            //emit event that repo was updated
            if(eventEmitter) {
                eventEmitter.emit(new RepositoryUpdateEvent(repositoryDefinition.repositoryName, artifactId))
            }

        } catch (Exception ex) {
            ex.printStackTrace()
            response.messages.add(new ResponseMessage(code:ResponseCodes.SERVER_ERROR,message: ex.message))
        }
        response
    }

    @PackageScope
    ResponseBatch uploadArtifactBinary(final RundeckRepositoryArtifact artifact, final InputStream artifactBinaryInputStream) {
        ResponseBatch response = new ResponseBatch()
        String binaryPath = BINARY_BASE+artifact.getArtifactBinaryFileName()
        Map meta = [:]
        try {
            def resource = DataUtil.withStream(artifactBinaryInputStream, meta, resourceFactory)
            storageTree.createResource(binaryPath, resource)

            response.messages.add(new ResponseMessage(code:ResponseCodes.SUCCESS))
        } catch (Exception ex) {
            ex.printStackTrace()
            response.messages.add(new ResponseMessage(code:ResponseCodes.SERVER_ERROR,message: ex.message))
        }
        response
    }

    @Override
    ManifestService getManifestService() {
        return manifestService
    }

    @Override
    void recreateAndSaveManifest() {
        manifestSource.saveManifest(manifestCreator.createManifest())
        manifestService.syncManifest()
    }

    @Override
    boolean isEnabled() {
        return repositoryDefinition.enabled
    }
}
