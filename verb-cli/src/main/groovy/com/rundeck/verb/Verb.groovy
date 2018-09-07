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

import com.lexicalscope.jewel.cli.Option
import com.rundeck.verb.artifact.ArtifactType
import com.rundeck.verb.client.RundeckVerbClient
import com.rundeck.verb.client.VerbClient
import com.rundeck.verb.client.util.ArtifactUtils
import org.rundeck.toolbelt.Arg
import org.rundeck.toolbelt.Command
import org.rundeck.toolbelt.CommandRunFailure
import org.rundeck.toolbelt.SubCommand
import org.rundeck.toolbelt.ToolBelt
import org.rundeck.toolbelt.input.jewelcli.JewelInput

@SubCommand
class Verb {

    private static final List<String> VALID_ARTIFACT_TYPES = ArtifactType.values().collect { ArtifactUtils.niceArtifactTypeName(it)}
    VerbClient client = new RundeckVerbClient()

    public static void main(String[] args) throws IOException, CommandRunFailure {
        ToolBelt.with("verb", new JewelInput(), new Verb()).runMain(args, true);
    }

    @Command(description = "Create a Verb artifact")
    public void create(CreateOpts createOpts) {
        if(!VALID_ARTIFACT_TYPES.contains(createOpts.artifactType)) {
            println "Artifact type must be one of: ${VALID_ARTIFACT_TYPES.join("|")}"
            return
        }
        def response = client.createArtifactTemplate(createOpts.artifactName,
                                                     ArtifactUtils.artifactTypeFromNice(createOpts.artifactType),
                                                     createOpts.serviceType,
                                                     createOpts.destinationDirectory)
        response.messages.each {
            println it.message
        }
    }

    interface CreateOpts {
        @Option(shortName = "n",description = "Artifact Name")
        String getArtifactName()
        @Option(shortName = "t",description = "Artifact Type")
        String getArtifactType()
        @Option(shortName = "s",description = "Rundeck Service Type")
        String getServiceType()
        @Option(shortName = "d",description = "The directory in which the artifact directory will be generated")
        String getDestinationDirectory()
    }

}
