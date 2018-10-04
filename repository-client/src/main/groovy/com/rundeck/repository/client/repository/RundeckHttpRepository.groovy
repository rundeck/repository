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

import com.rundeck.repository.ResponseBatch
import com.rundeck.repository.ResponseCodes
import com.rundeck.repository.ResponseMessage
import com.rundeck.repository.artifact.RepositoryArtifact
import com.rundeck.repository.client.artifact.RundeckRepositoryArtifact
import com.rundeck.repository.client.manifest.HttpManifestSource
import com.rundeck.repository.client.manifest.MemoryManifestService
import com.rundeck.repository.client.util.ArtifactFileset
import com.rundeck.repository.client.util.ArtifactUtils
import com.rundeck.repository.client.validators.BinaryValidator
import com.rundeck.repository.definition.RepositoryDefinition
import com.rundeck.repository.api.ArtifactRepository
import com.rundeck.repository.manifest.ManifestEntry
import com.rundeck.repository.manifest.ManifestService
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.MediaType
import okhttp3.Response


class RundeckHttpRepository implements ArtifactRepository {

    private OkHttpClient client = new OkHttpClient();

    RepositoryDefinition repositoryDefinition
    ManifestService manifestService
    String rundeckRepositoryEndpoint

    RundeckHttpRepository(RepositoryDefinition repoDef) {
        if(!repoDef.configProperties.rundeckRepoEndpoint) throw new Exception("Rundeck repository endpoint must be provided by setting configProperties.rundeckRepoEndpoint in repository definition.")
        this.rundeckRepositoryEndpoint = repoDef.configProperties.rundeckRepoEndpoint
        this.repositoryDefinition = repoDef
        this.manifestService = new MemoryManifestService(new HttpManifestSource(new URL(rundeckRepositoryEndpoint+ "/manifest")))
    }

    @Override
    RepositoryDefinition getRepositoryDefinition() {
        return repositoryDefinition
    }

    @Override
    RepositoryArtifact getArtifact(final String artifactId, final String version = null) {
        Response response
        try {
            String artifactVer = version ?: manifestService.getEntry(artifactId).currentVersion
            Request rq = new Request.Builder().method("GET",null).url(rundeckRepositoryEndpoint+ "/artifact/${ArtifactUtils.artifactMetaFileName(artifactId, artifactVer)}".toString()).build()
            response = client.newCall(rq).execute()
            return ArtifactUtils.createArtifactFromYamlStream(response.body().byteStream())
        } finally {
            if(response) response.body().close()
        }
    }

    @Override
    InputStream getArtifactBinary(final String artifactId, final String version = null) {
        ManifestEntry entry = manifestService.getEntry(artifactId)
        String artifactVer = version ?: entry.currentVersion
        String extension = ArtifactUtils.artifactTypeFromNice(entry.artifactType).extension
        Request rq = new Request.Builder().method("GET",null).url(rundeckRepositoryEndpoint+ "/binary/${ArtifactUtils.artifactBinaryFileName(artifactId, artifactVer, extension)}".toString()).build()
        Response response = client.newCall(rq).execute()
        return response.body().byteStream()
    }

    @Override
    ResponseBatch saveNewArtifact(final RepositoryArtifact artifact) {
        ResponseBatch rbatch = new ResponseBatch()
        ByteArrayOutputStream baos = new ByteArrayOutputStream()
        ArtifactUtils.saveArtifactToOutputStream(artifact,baos)
        RequestBody body = RequestBody.create(MediaType.parse("application/octet-stream"), baos.toByteArray())
        Response response = null
        try {
            OkHttpClient client = new OkHttpClient();
            Request rq = new Request.Builder().method("POST", body).url(rundeckRepositoryEndpoint+ "/upload/artifact/${artifact.artifactMetaFileName}".toString()).build()
            response = client.newCall(rq).execute()
            rbatch.addMessage(ResponseMessage.success())
        } catch(Exception ex) {
            ex.printStackTrace()
            rbatch.addMessage(new ResponseMessage(code: ResponseCodes.META_UPLOAD_FAILED))
        } finally {
            if(response) response.body().close()
        }
        return rbatch
    }

    @Override
    ResponseBatch uploadArtifact(final InputStream artifactInputStream) {
        ArtifactFileset artifactFileset = ArtifactUtils.constructArtifactFileset(artifactInputStream)
        ResponseBatch rbatch = new ResponseBatch()
        rbatch.messages.addAll(BinaryValidator.validate(artifactFileset.artifact.artifactType, uploadTmp).messages)
        if(rbatch.batchSucceeded()) {
            rbatch.messages.addAll(saveNewArtifact(artifactFileset.artifact).messages)
            rbatch.messages.addAll(
                    uploadArtifactBinary(
                            artifactFileset.artifact,
                            artifactFileset.artifactBinary
                    ).messages
            )
        }
        return rbatch
    }

    ResponseBatch uploadArtifactBinary(final RundeckRepositoryArtifact artifact, final File binaryFile) {
        ResponseBatch rbatch = new ResponseBatch()
        RequestBody body = RequestBody.create(MediaType.parse("application/octet-stream"), binaryFile)
        Response response = null
        try {
            Request rq = new Request.Builder().method("POST", body).url(rundeckRepositoryEndpoint+ "/upload/binary/${artifact.artifactBinaryFileName}".toString()).build()
            response = client.newCall(rq).execute()
            rbatch.addMessage(ResponseMessage.success())
        } catch(Exception ex) {
            ex.printStackTrace()
            rbatch.addMessage(new ResponseMessage(code: ResponseCodes.BINARY_UPLOAD_FAILED))
        } finally {
            if(response) response.body().close()
        }
        return rbatch
    }

    @Override
    ManifestService getManifestService() {
        return manifestService
    }

    @Override
    void recreateAndSaveManifest() {
        //no-op
    }

    @Override
    boolean isEnabled() {
        return repositoryDefinition.enabled
    }
}