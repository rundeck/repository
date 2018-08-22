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
package com.rundeck.verb

import com.rundeck.verb.artifact.ArtifactType
import com.rundeck.verb.client.RundeckVerbClient
import com.rundeck.verb.client.RundeckVerbConfigurationProperties
import com.rundeck.verb.client.VerbClient
import com.rundeck.verb.client.repository.RundeckRepositoryManager
import com.rundeck.verb.client.util.ArtifactUtils


class App {

    VerbClient client

    App() {
        client = new RundeckVerbClient(repositoryManager: new RundeckRepositoryManager())
        println "Registered repositories: " + client.repositoryManager.listRepositories().join(",")
    }

    static void main(String[] args) {
        List<String> pargs = args.toList()
        App app = new App()
        if(pargs.isEmpty()) app.printUsage()
        else app.runCommand(pargs)
    }

    void printUsage() {
        println "create ArtifactName [${ArtifactType.values().collect { ArtifactUtils.niceArtifactTypeName(it)}.join("|")}] serviceType destinationPath"
        println "upload repositoryName pathToArtifact"
    }

    void runCommand(final List<String> args) {
        String cmd = args.remove(0)
        ResponseBatch response = new ResponseBatch()
        switch (cmd.toLowerCase()) {
            case "create":
                String baseDir = args.size() == 4 ? args[3] : RundeckVerbClient.clientProperties[RundeckVerbConfigurationProperties.DEV_BASE]
                if(args.size() < 3 || !baseDir){
                    printUsage()
                    break
                }
                response = client.createArtifactTemplate(args[0], ArtifactUtils.artifactTypeFromNice(args[1]),args[2],baseDir)
                break
            case "upload":
                if(args.size() != 2){
                    printUsage()
                    break
                }
                response = client.uploadArtifact(args[0],new File(args[1]).newInputStream())
                break
            default:
                printUsage()
        }
        response.messages.each {
            println it
        }
    }
}
