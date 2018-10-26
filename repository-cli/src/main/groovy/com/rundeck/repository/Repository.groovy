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
package com.rundeck.repository

import com.lexicalscope.jewel.cli.Option
import com.rundeck.repository.client.util.ArtifactFileset
import com.rundeck.repository.client.util.ArtifactUtils
import groovy.json.JsonSlurper
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.rundeck.toolbelt.Command
import org.rundeck.toolbelt.CommandOutput
import org.rundeck.toolbelt.CommandRunFailure
import org.rundeck.toolbelt.SubCommand
import org.rundeck.toolbelt.ToolBelt
import org.rundeck.toolbelt.input.jewelcli.JewelInput

@SubCommand
class Repository {

    String officialRundeckRepoArtifactSaveUrl = "https://2n2gfj5lgh.execute-api.us-east-1.amazonaws.com/dev/save"
    String officialRundeckRepoSubmissionUrl = "https://2n2gfj5lgh.execute-api.us-east-1.amazonaws.com/dev/upload"

    public static void main(String[] args) throws IOException, CommandRunFailure {
        ToolBelt.with("repository", new JewelInput(), new Repository()).runMain(args, true);
    }

    @Command(description = "Submit artifact to official Rundeck Repository")
    public void submit(SubmitOpts submitOpts, CommandOutput output) {
        File artifactFile = new File(submitOpts.getArtifactFilePath())
        if(!artifactFile.exists()) throw new Exception("Artifact file: ${submitOpts.getArtifactFilePath()} cannot be found")
        ArtifactFileset artifactFileset = ArtifactUtils.constructArtifactFileset(artifactFile.newInputStream())
        OkHttpClient client = new OkHttpClient()
        RequestBody rqBody = RequestBody.create(MediaType.parse("application/octet-stream"), artifactFileset.artifactBinary)
        Request rq = new Request.Builder().url(officialRundeckRepoSubmissionUrl+
                                               "/"+artifactFileset.artifact.artifactType.name().toLowerCase()+
                                               "/"+artifactFileset.artifact.id+
                                               "/"+artifactFileset.artifact.version)
                                          .post(rqBody)
                                          .build()
        output.output("Submitting ${artifactFileset.artifact.id} - ${artifactFileset.artifact.name} - ${artifactFileset.artifact.version}")
        Response response = client.newCall(rq).execute()
        if(response.code() == 200) {
            RequestBody metaBody = RequestBody.create(MediaType.parse("application/json"), ArtifactUtils.artifactToJson(artifactFileset.artifact))
            Request rqMeta = new Request.Builder().url(officialRundeckRepoArtifactSaveUrl)
                                                      .post(metaBody)
                                                      .build()
            Response metaSaveResponse = client.newCall(rqMeta).execute()
            JsonSlurper jsonSlurper = new JsonSlurper()
            def json = jsonSlurper.parse(metaSaveResponse.body().charStream())
            output.output(json.msg)
        } else {
            output.error("Upload failed")
            output.error(response.body().charStream().text)
        }

    }

    interface SubmitOpts {
        @Option(shortName = "a",description = "Author id")
        String getAuthorId()
        @Option(shortName = "t",description = "Author developer token")
        String getAuthorToken()
        @Option(shortName = "f",description = "Path to artifact file")
        String getArtifactFilePath()

    }

}
