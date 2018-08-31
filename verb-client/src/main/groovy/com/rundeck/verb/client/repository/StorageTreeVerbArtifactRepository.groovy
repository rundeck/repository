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
package com.rundeck.verb.client.repository

import com.dtolabs.rundeck.core.storage.StorageTree
import com.rundeck.verb.ResponseBatch
import com.rundeck.verb.ResponseCodes
import com.rundeck.verb.ResponseMessage
import com.rundeck.verb.artifact.VerbArtifact
import com.rundeck.verb.client.artifact.RundeckVerbArtifact
import com.rundeck.verb.client.manifest.MemoryManifestService
import com.rundeck.verb.client.manifest.StorageTreeManifestCreator
import com.rundeck.verb.client.manifest.StorageTreeManifestSource
import com.rundeck.verb.client.util.ArtifactFileset
import com.rundeck.verb.client.util.ArtifactUtils
import com.rundeck.verb.client.util.ResourceFactory
import com.rundeck.verb.events.RepositoryEventEmitter
import com.rundeck.verb.events.RepositoryUpdateEvent
import com.rundeck.verb.manifest.ManifestEntry
import com.rundeck.verb.manifest.ManifestService
import com.rundeck.verb.manifest.ManifestSource
import com.rundeck.verb.repository.RepositoryDefinition
import com.rundeck.verb.repository.VerbArtifactRepository
import groovy.transform.PackageScope
import org.rundeck.storage.data.DataUtil

class StorageTreeVerbArtifactRepository implements VerbArtifactRepository {
    static final String ARTIFACT_BASE = "artifacts/"
    static final String BINARY_BASE = "binary/"
    private StorageTree storageTree
    RepositoryDefinition repositoryDefinition
    private final ManifestService manifestService
    private final RepositoryEventEmitter eventEmitter
    private StorageTreeManifestCreator manifestCreator
    static final ResourceFactory resourceFactory = new ResourceFactory()
    private ManifestSource manifestSource

    StorageTreeVerbArtifactRepository(StorageTree storageTree, RepositoryDefinition repositoryDefinition, RepositoryEventEmitter eventEmitter) {
        this(storageTree,repositoryDefinition)
        this.eventEmitter = eventEmitter

    }

    StorageTreeVerbArtifactRepository(StorageTree storageTree, RepositoryDefinition repositoryDefinition) {
        if(!storageTree) throw new Exception("Unable to initialize storage tree repository. No storage tree provided.")
        if(!repositoryDefinition.configProperties.manifestPath) throw new Exception("Path to manifest in storage tree must be provided by setting configProperties.manifestPath in repository definition.")
        this.storageTree = storageTree
        this.repositoryDefinition = repositoryDefinition
        this.manifestSource = new StorageTreeManifestSource(storageTree,this.repositoryDefinition.configProperties.manifestPath)
        this.manifestService = new MemoryManifestService(manifestSource)
        manifestCreator = new StorageTreeManifestCreator(storageTree)
    }

    @Override
    RepositoryDefinition getRepositoryDefinition() {
        return repositoryDefinition
    }

    @Override
    VerbArtifact getArtifact(final String artifactId, final String version = null) {
        String artifactVer = version ?: manifestService.getEntry(artifactId).currentVersion
        ArtifactUtils.createArtifactFromStream(storageTree.getResource(ARTIFACT_BASE+ArtifactUtils.artifactMetaFileName(artifactId, artifactVer)).contents.inputStream)
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
        //responseBatch.messages.addAll(validateArtifactBinary(uploadTmp))
        if(!responseBatch.batchSucceeded()) return responseBatch

        responseBatch.messages.addAll(uploadArtifactMeta(artifactFileset.artifact, artifactFileset.artifactMeta.newInputStream()).messages)
        if(artifactFileset.hasBinary()) {
            responseBatch.messages.addAll(uploadArtifactBinary(artifactFileset.artifact, artifactFileset.artifactBinary.newInputStream()).messages)
        }
        responseBatch
    }

    @PackageScope
    ResponseBatch uploadArtifactMeta(final RundeckVerbArtifact artifact, final InputStream metaInputStream) {
        ResponseBatch response = new ResponseBatch()
        String artifactPath = ARTIFACT_BASE + artifact.getArtifactMetaFileName()
        Map meta = [:]
        try {
            def resource = DataUtil.withStream(metaInputStream, meta, resourceFactory)
            storageTree.createResource(artifactPath, resource)

            response.messages.add(new ResponseMessage(code:ResponseCodes.SUCCESS))
            //recreate manifest
            recreateAndSaveManifest()
            manifestService.syncManifest()
            //emit event that repo was updated
            if(eventEmitter) {
                eventEmitter.emit(new RepositoryUpdateEvent(repositoryDefinition.repositoryName,artifactId))
            }

        } catch (Exception ex) {
            ex.printStackTrace()
            response.messages.add(new ResponseMessage(code:ResponseCodes.SERVER_ERROR,message: ex.message))
        }
        response
    }

    @PackageScope
    ResponseBatch uploadArtifactBinary(final RundeckVerbArtifact artifact, final InputStream artifactBinaryInputStream) {
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

    void recreateAndSaveManifest() {
        manifestSource.saveManifest(manifestCreator.createManifest())
    }

}
