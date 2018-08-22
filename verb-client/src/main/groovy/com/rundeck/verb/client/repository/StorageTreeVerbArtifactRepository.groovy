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

import com.dtolabs.rundeck.core.storage.ResourceMeta
import com.dtolabs.rundeck.core.storage.StorageUtil
import com.fasterxml.jackson.databind.ObjectMapper
import com.rundeck.verb.ResponseBatch
import com.rundeck.verb.ResponseCodes
import com.rundeck.verb.ResponseMessage
import com.rundeck.verb.artifact.ArtifactType
import com.rundeck.verb.artifact.VerbArtifact
import com.rundeck.verb.client.artifact.RundeckVerbArtifact
import com.rundeck.verb.client.manifest.StorageTreeManifestCreator
import com.rundeck.verb.client.util.ArtifactFileset
import com.rundeck.verb.client.util.ArtifactUtils
import com.rundeck.verb.events.RepositoryEventEmitter
import com.rundeck.verb.events.RepositoryUpdateEvent
import com.rundeck.verb.manifest.ArtifactManifest
import com.rundeck.verb.manifest.ManifestEntry
import com.rundeck.verb.manifest.ManifestService
import com.rundeck.verb.repository.RepositoryDefinition
import com.rundeck.verb.repository.VerbArtifactRepository
import com.rundeck.verb.util.TempFileProvider
import groovy.transform.PackageScope
import org.rundeck.storage.api.Tree
import org.rundeck.storage.data.DataUtil

class StorageTreeVerbArtifactRepository implements VerbArtifactRepository {
    static final String ARTIFACT_BASE = "artifacts/"
    static final String BINARY_BASE = "binary/"
    Tree<ResourceMeta> storageTree
    RepositoryDefinition repositoryDefinition
    private final ManifestService manifestService
    private final RepositoryEventEmitter eventEmitter
    private StorageTreeManifestCreator manifestCreator

    StorageTreeVerbArtifactRepository(Tree<ResourceMeta> storageTree, RepositoryDefinition repositoryDefinition, ManifestService manifestService, RepositoryEventEmitter eventEmitter) {
        this(storageTree,repositoryDefinition,manifestService)
        this.eventEmitter = eventEmitter

    }

    StorageTreeVerbArtifactRepository(Tree<ResourceMeta> storageTree, RepositoryDefinition repositoryDefinition, ManifestService manifestService) {
        this.manifestService = manifestService
        this.storageTree = storageTree
        this.repositoryDefinition = repositoryDefinition
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
        String extension = ArtifactUtils.artifactTypeFromNice(entry.artifactType)
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
        String artifactPath = ARTIFACT_BASE + ArtifactUtils.artifactMetaFileName(artifact)
        Map meta = [:]
        try {
            def resource = DataUtil.withStream(metaInputStream, meta, StorageUtil.factory())
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
        String binaryPath = BINARY_BASE+ArtifactUtils.artifactBinaryFileName(artifact)
        Map meta = [:]
        try {
            def resource = DataUtil.withStream(artifactBinaryInputStream, meta, StorageUtil.factory())
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
        //TODO: this assumes the URL used in the repo defn is a writable location.
        ArtifactUtils.writeArtifactManifestToFile(manifestCreator.createManifest(),new File(repositoryDefinition.manifestLocation.toURI()).newOutputStream())
    }
}
