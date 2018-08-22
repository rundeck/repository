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
import com.rundeck.verb.ResponseBatch
import com.rundeck.verb.artifact.VerbArtifact
import com.rundeck.verb.client.artifact.RundeckVerbArtifact
import com.rundeck.verb.client.signing.GpgPassphraseProvider
import com.rundeck.verb.client.signing.GpgTools
import com.rundeck.verb.client.util.ArtifactFileset
import com.rundeck.verb.client.util.ArtifactUtils
import com.rundeck.verb.events.RepositoryEventEmitter
import com.rundeck.verb.manifest.ManifestEntry
import com.rundeck.verb.manifest.ManifestService
import com.rundeck.verb.repository.RepositoryDefinition
import org.rundeck.storage.api.Tree
import org.rundeck.storage.data.DataUtil


class GpgSignedStorageTreeVerbArtifactRepository extends StorageTreeVerbArtifactRepository {
    private static final String SIG_SUFFIX = ".sig"
    File gpgPublicKey
    File gpgPrivateKey

    GpgPassphraseProvider passphraseProvider

    GpgSignedStorageTreeVerbArtifactRepository(
            final File gpgPublicKey,
            final File gpgPrivateKey,
            final GpgPassphraseProvider passphraseProvider,
            final Tree<ResourceMeta> storageTree,
            final RepositoryDefinition repositoryDefinition,
            final ManifestService manifestService,
            final RepositoryEventEmitter eventEmitter
    ) {
        super(storageTree, repositoryDefinition, manifestService, eventEmitter)
        this.gpgPublicKey = gpgPublicKey
        this.gpgPrivateKey = gpgPrivateKey
        this.passphraseProvider = passphraseProvider
    }

    GpgSignedStorageTreeVerbArtifactRepository(
            final File gpgPublicKey,
            final File gpgPrivateKey,
            final GpgPassphraseProvider passphraseProvider,
            final Tree<ResourceMeta> storageTree,
            final RepositoryDefinition repositoryDefinition,
            final ManifestService manifestService
    ) {
        this(gpgPublicKey, gpgPrivateKey, passphraseProvider, storageTree, repositoryDefinition, manifestService, null)
    }

    @Override
    VerbArtifact getArtifact(final String artifactId, final String version = null) {
       String artifactVer = version ?: manifestService.getEntry(artifactId).currentVersion
       String metaPath = ArtifactUtils.artifactMetaFileName(artifactId,artifactVer)
       InputStream artifactFile = storageTree.getResource(ARTIFACT_BASE+ metaPath).contents.inputStream
       InputStream artifactSig = storageTree.getResource(ARTIFACT_BASE+ metaPath+SIG_SUFFIX).contents.inputStream
       GpgTools.validateSignature(artifactFile, artifactSig, gpgPublicKey.newInputStream())
       return super.getArtifact(artifactId)
    }

    @Override
    InputStream getArtifactBinary(final String artifactId, final String version = null) {
        ManifestEntry entry = manifestService.getEntry(artifactId)
        String artifactVer = version ?: entry.currentVersion
        String extension = ArtifactUtils.artifactTypeFromNice(entry.artifactType)
        String binaryPath = ArtifactUtils.artifactBinaryFileName(artifactId,artifactVer,extension)
        InputStream artifactFile = storageTree.getResource(ARTIFACT_BASE+ binaryPath).contents.inputStream
        InputStream artifactSig = storageTree.getResource(ARTIFACT_BASE+ binaryPath+SIG_SUFFIX).contents.inputStream
        GpgTools.validateSignature(artifactFile, artifactSig, gpgPublicKey.newInputStream())
        return super.getArtifactBinary(artifactId)
    }

    @Override
    ResponseBatch uploadArtifact(final InputStream artifactInputStream) {
        if(!gpgPrivateKey) return super.uploadArtifact(artifactInputStream)

        ResponseBatch responseBatch = new ResponseBatch()
        ArtifactFileset artifactFileset = ArtifactUtils.constructArtifactFileset(artifactInputStream)
        //responseBatch.messages.addAll(validateArtifactBinary(uploadTmp))
        if(!responseBatch.batchSucceeded()) return responseBatch

        responseBatch.messages.addAll(uploadArtifactMeta(artifactFileset.artifact, artifactFileset.artifactMeta.newInputStream()).messages)
        if(artifactFileset.hasBinary()) {
            File binarySig = File.createTempFile("binary","sig")
            GpgTools.signDetached(false,gpgPrivateKey.newInputStream(),artifactFileset.artifactBinary.newInputStream(),binarySig.newOutputStream(),passphraseProvider)
            String sigPath = BINARY_BASE+ArtifactUtils.artifactBinaryFileName(artifactFileset.artifact)+".sig"
            def sigResource = DataUtil.withStream(binarySig.newInputStream(), [:], StorageUtil.factory())
            storageTree.createResource(sigPath,sigResource)
            responseBatch.messages.addAll(uploadArtifactBinary(artifactFileset.artifact, artifactFileset.artifactBinary.newInputStream()).messages)
        }
        responseBatch

        return null
    }

    @Override
    ResponseBatch uploadArtifactMeta(final RundeckVerbArtifact artifact, final InputStream metaInputStream) {
        InputStream artifactStream
        if(gpgPrivateKey) {
            ByteArrayOutputStream bout = new ByteArrayOutputStream()
            bout << metaInputStream
            File metaSig = File.createTempFile("meta","sig")
            GpgTools.signDetached(false,gpgPrivateKey.newInputStream(),new ByteArrayInputStream(bout.toByteArray()),metaSig.newOutputStream(),passphraseProvider)
            String sigPath = ARTIFACT_BASE+ArtifactUtils.artifactMetaFileName(artifact)+".sig"
            def sigResource = DataUtil.withStream(metaSig.newInputStream(), [:], StorageUtil.factory())
            storageTree.createResource(sigPath,sigResource)
            artifactStream = new ByteArrayInputStream(bout.toByteArray())
        } else {
            artifactStream = metaInputStream
        }

        return super.uploadArtifactMeta(artifactId,artifactStream)
    }

}
