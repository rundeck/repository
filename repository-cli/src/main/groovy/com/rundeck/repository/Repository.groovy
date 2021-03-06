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

import com.dtolabs.rundeck.core.storage.StorageConverterPluginAdapter
import com.dtolabs.rundeck.core.storage.StorageTimestamperConverter
import com.dtolabs.rundeck.core.storage.StorageTree
import com.dtolabs.rundeck.core.storage.StorageUtil
import com.fasterxml.jackson.databind.ObjectMapper
import com.lexicalscope.jewel.cli.Option
import com.rundeck.repository.api.RepositoryOwner
import com.rundeck.repository.api.RepositoryType
import com.rundeck.repository.client.repository.StorageTreeArtifactRepository
import com.rundeck.repository.client.util.ArtifactFileset
import com.rundeck.repository.client.util.ArtifactUtils
import com.rundeck.repository.client.util.ResourceFactory
import com.rundeck.repository.definition.RepositoryDefinition

import org.rundeck.storage.conf.TreeBuilder
import org.rundeck.storage.data.file.FileTreeUtil
import org.rundeck.toolbelt.Command
import org.rundeck.toolbelt.CommandOutput
import org.rundeck.toolbelt.CommandRunFailure
import org.rundeck.toolbelt.SubCommand
import org.rundeck.toolbelt.ToolBelt
import org.rundeck.toolbelt.input.jewelcli.JewelInput

@SubCommand
class Repository {
    private static final String UNAUTHORIZED_MSG = "\"message\":\"Unauthorized\""
    private static final String EXPIRED_MSG = "\"message\":\"The incoming token has expired\""
    private static ObjectMapper mapper = new ObjectMapper()
    private String repoInfoUrlOverride = System.getProperty("repoInfoUrl")
    private String repo = System.getProperty("repo","oss")
    private String stageOverride = System.getProperty("stage","")

    public static void main(String[] args) throws IOException, CommandRunFailure {
        ToolBelt.with("repository", new JewelInput(), new Repository()).runMain(args, true);
    }

    @Command(description = "Submit artifact to official Rundeck Repository")
    public void submit(SubmitOpts submitOpts, CommandOutput output) {

        File artifactFile = new File(submitOpts.getArtifactFilePath())
        if(!artifactFile.exists()) throw new Exception("Artifact file: ${submitOpts.getArtifactFilePath()} cannot be found")
        ArtifactFileset artifactFileset = ArtifactUtils.constructArtifactFileset(artifactFile)
        if(!artifactFileset.artifact.name) throw new Exception("Plugin metadata must contain the plugin name.")
        if(!artifactFileset.artifact.version) throw new Exception("Plugin metadata must contain the plugin version.")

        String stage = stageOverride ? "-${stageOverride}" : ""
        String repoInfoBase = repoInfoUrlOverride ?: "https://api${stage}.rundeck.com/repo/v1/${repo}"

        RepoInfo repoInfo = RepositoryUtils.getRepoInfo(repoInfoBase+"/service-info")
        if(!repoInfo) throw new Exception("Unable to obtain repository information")
        String accessToken = tokenFromCache() ?:  RepositoryUtils.loginViaConsoleAndGetAccessToken(repoInfo)
        String artifactUploadUrl = repoInfoBase + "/artifact"

        def artifactUploadPayload = [id:artifactFileset.artifact.id,
                                     name:artifactFileset.artifact.name,
                                     version: artifactFileset.artifact.version,
                                     extension: artifactFileset.artifact.artifactType.extension
                                    ]
        String resp = ""
        try {
            resp = getUploadUrls(artifactUploadUrl,accessToken,mapper.writeValueAsString(artifactUploadPayload))
        } catch(Exception ex) {
            if(ex.message.contains(UNAUTHORIZED_MSG) || ex.message.contains(EXPIRED_MSG)) {
                //if failed with token exception try login and call again
                accessToken = RepositoryUtils.loginViaConsoleAndGetAccessToken(repoInfo)
                resp = getUploadUrls(artifactUploadUrl,accessToken,mapper.writeValueAsString(artifactUploadPayload))
            } else {
                throw ex
            }
        }
        def signed = mapper.readValue(resp,Map)
        if(signed.submissionId) {
            output.output("Submitting ${artifactFileset.artifact.id} : ${artifactFileset.artifact.name} : ${artifactFileset.artifact.version}")
            //output.output("Submission Id: " + signed.submissionId)
            //output.output("Track the progress of this submission online at: https://online.rundeck.com/")
        }
        UploadOperationResult metaResult = null
        UploadOperationResult binaryResult = null

        if(signed.metaUrl) {
            File tmp = File.createTempFile("tmp","meta")
            tmp << ArtifactUtils.artifactToJson(artifactFileset.artifact)
            metaResult = RepositoryUtils.upload(
                    signed.metaUrl,
                    tmp.absolutePath,
                    'application/json'
            )
        }
        if(metaResult?.successful && signed.binaryUrl) {
            binaryResult = RepositoryUtils.upload(
                    signed.binaryUrl,
                    artifactFile.absolutePath
            )
        }

        if(metaResult?.successful && binaryResult?.successful) {
            output.output("Artifact uploaded successfully")
        } else {
            output.error("Artifact failed to upload")
            if(!metaResult?.successful && metaResult?.msg) output.output(metaResult.msg)
            if(!metaResult?.successful && metaResult?.msg) output.output(binaryResult.msg)
        }

    }

    private String tokenFromCache() {
        File tokenCache = new File(System.getProperty("user.home")+RepositoryUtils.TOKEN_CACHE_PATH)
        if(tokenCache.exists()) return tokenCache.text
        return null
    }

    private String getUploadUrls(artifactUploadUrl,accessToken,payload) {
        return RepositoryUtils.callAPIGwWithAccessToken(
                artifactUploadUrl,
                accessToken,
                payload
        )
    }

    interface SubmitOpts {
        @Option(shortName = "f",description = "Path to artifact file")
        String getArtifactFilePath()

    }

    @Command(description = "Create a local Storage Tree Repository from plugins in a directory")
    void createRepo(CreateRepoOpts opts, CommandOutput output) {
        File inputDir = new File(opts.getInputDir())
        File outputBaseDir = new File(opts.getOutputDir())
        if(!inputDir.exists()) throw new Exception("Input directory ${opts.getInputDir()} does not exist")

        TreeBuilder tbuilder = TreeBuilder.builder(FileTreeUtil.forRoot(outputBaseDir, new ResourceFactory()))
        StorageTree tree = StorageUtil.asStorageTree(tbuilder.convert(new StorageConverterPluginAdapter(
                "builtin:timestamp",
                new StorageTimestamperConverter()
        )).build())
        RepositoryDefinition repoDef = new RepositoryDefinition()
        repoDef.repositoryName = "new-repo"
        repoDef.configProperties.manifestType = "tree"
        repoDef.configProperties.manifestPath = "manifest.json"
        repoDef.type = RepositoryType.STORAGE_TREE
        repoDef.owner = RepositoryOwner.PRIVATE

        StorageTreeArtifactRepository repo = new StorageTreeArtifactRepository(tree,repoDef)

        inputDir.traverse(type: groovy.io.FileType.FILES,nameFilter: ~/.*[\.jar|\.zip]$/) { file ->
            println "Processing ${file.name}"
            def resp = repo.uploadArtifact(file.newInputStream())
            if(!resp.batchSucceeded()) {
                output.error(resp.messages)
            }
        }
        inputDir.traverse(type: groovy.io.FileType.FILES,nameFilter: ~/.*[\.yaml]$/) { file ->
            println "Processing ${file.name}"
            def resp = repo.saveNewArtifact(ArtifactUtils.createArtifactFromYamlStream(file.newInputStream()))
            if(!resp.batchSucceeded()) {
                output.error(resp.messages)
            }
        }


        output.output("Your repository is ready at: ${outputBaseDir}")

    }

    interface CreateRepoOpts {
        @Option(shortName = "i",description = "Directory containing plugins")
        String getInputDir()
        @Option(shortName = "o",description = "Base Path to directory containing repository")
        String getOutputDir()
    }

    @Command(description = "Print the ID of an artifact")
    void identify(IdentifyOpts opts, CommandOutput output) {
        def fileset = ArtifactUtils.constructArtifactFileset(new File(opts.artifactFilePath))
        output.output(fileset.artifact.id + " : " + fileset.artifact.name + " : " + fileset.artifact.version)
    }

    interface IdentifyOpts {
        @Option(shortName = "f",description = "Path to artifact file")
        String getArtifactFilePath()

    }
}
