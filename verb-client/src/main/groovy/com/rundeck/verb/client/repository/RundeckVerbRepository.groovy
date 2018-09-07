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

import com.rundeck.verb.ResponseBatch
import com.rundeck.verb.ResponseCodes
import com.rundeck.verb.ResponseMessage
import com.rundeck.verb.artifact.VerbArtifact
import com.rundeck.verb.client.artifact.RundeckVerbArtifact
import com.rundeck.verb.client.manifest.MemoryManifestService
import com.rundeck.verb.client.util.ArtifactFileset
import com.rundeck.verb.client.util.ArtifactUtils
import com.rundeck.verb.manifest.ManifestEntry
import com.rundeck.verb.manifest.ManifestService
import com.rundeck.verb.repository.RepositoryDefinition
import com.rundeck.verb.repository.VerbArtifactRepository
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.MediaType
import okhttp3.Response


class RundeckVerbRepository implements VerbArtifactRepository {

    private OkHttpClient client = new OkHttpClient();

    RepositoryDefinition repositoryDefinition
    ManifestService manifestService
    String rundeckVerbEndpoint

    RundeckVerbRepository(String rundeckVerbEndpoint) {
        this.rundeckVerbEndpoint = rundeckVerbEndpoint
        this.manifestService = new MemoryManifestService(rundeckVerbEndpoint+"/manifest")
    }

    @Override
    RepositoryDefinition getRepositoryDefinition() {
        return repositoryDefinition
    }

    @Override
    VerbArtifact getArtifact(final String artifactId, final String version = null) {
        Response response
        try {
            String artifactVer = version ?: manifestService.getEntry(artifactId).currentVersion
            Request rq = new Request.Builder().method("GET",null).url(rundeckVerbEndpoint+"/artifact/${ArtifactUtils.artifactMetaFileName(artifactId,artifactVer)}".toString()).build()
            response = client.newCall(rq).execute()
            return ArtifactUtils.createArtifactFromStream(response.body().byteStream())
        } finally {
            if(response) response.body().close()
        }
    }

    @Override
    InputStream getArtifactBinary(final String artifactId, final String version = null) {
        ManifestEntry entry = manifestService.getEntry(artifactId)
        String artifactVer = version ?: entry.currentVersion
        String extension = ArtifactUtils.artifactTypeFromNice(entry.artifactType).extension
        Request rq = new Request.Builder().method("GET",null).url(rundeckVerbEndpoint+"/binary/${ArtifactUtils.artifactBinaryFileName(artifactId,artifactVer,extension)}".toString()).build()
        Response response = client.newCall(rq).execute()
        return response.body().byteStream()
    }

    @Override
    ResponseBatch uploadArtifact(final InputStream artifactInputStream) {
        ArtifactFileset artifactFileset = ArtifactUtils.constructArtifactFileset(artifactInputStream)
        ResponseBatch rbatch = new ResponseBatch()
        rbatch.messages.addAll(uploadArtifactMeta(artifactFileset.artifact,artifactFileset.artifactMeta).messages)
        if(artifactFileset.hasBinary()) {
            rbatch.messages.addAll(uploadArtifactBinary(artifactFileset.artifact,artifactFileset.artifactBinary).messages)
        }

        return rbatch
    }

    ResponseBatch uploadArtifactBinary(final RundeckVerbArtifact rundeckVerbArtifact, final File binaryFile) {
        ResponseBatch rbatch = new ResponseBatch()
        RequestBody body = RequestBody.create(MediaType.parse("application/octet-stream"), binaryFile)
        Response response = null
        try {
            Request rq = new Request.Builder().method("POST", body).url(rundeckVerbEndpoint+"/upload/binary/${ArtifactUtils.artifactBinaryFileName(rundeckVerbArtifact)}".toString()).build()
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

    ResponseBatch uploadArtifactMeta(final RundeckVerbArtifact rundeckVerbArtifact, final File metaFile) {
        ResponseBatch rbatch = new ResponseBatch()
        RequestBody body = RequestBody.create(MediaType.parse("application/octet-stream"), metaFile)
        Response response = null
        try {
            OkHttpClient client = new OkHttpClient();
            Request rq = new Request.Builder().method("POST", body).url(rundeckVerbEndpoint+"/upload/artifact/${ArtifactUtils.artifactMetaFileName(rundeckVerbArtifact)}".toString()).build()
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
    ManifestService getManifestService() {
        return manifestService
    }

    @Override
    void recreateAndSaveManifest() {
        //no-op
    }
}
