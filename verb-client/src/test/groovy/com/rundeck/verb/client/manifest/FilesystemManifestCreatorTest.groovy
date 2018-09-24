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
package com.rundeck.verb.client.manifest

import com.dtolabs.rundeck.core.plugins.PluginUtils
import com.google.common.io.Files
import com.rundeck.plugin.template.FilesystemArtifactTemplateGenerator
import com.rundeck.plugin.template.PluginType
import com.rundeck.verb.client.TestUtils
import com.rundeck.verb.client.util.ArtifactUtils
import com.rundeck.verb.manifest.ArtifactManifest
import com.rundeck.verb.manifest.ManifestEntry
import spock.lang.Specification


class FilesystemManifestCreatorTest extends Specification {

    def "Create Manifests Handling multiple versions"() {
        setup:
        File tempManifestDir = File.createTempDir()
        File tempScriptDir = File.createTempDir()
        FilesystemArtifactTemplateGenerator generator = new FilesystemArtifactTemplateGenerator()
        String artifactId = PluginUtils.generateShaIdFromName("Script Plugin Multiver")
        generator.generate("Script Plugin Multiver", PluginType.script, "NodeExecutor", tempScriptDir.absolutePath)
        TestUtils.zipDir(tempScriptDir.absolutePath+"/script-plugin-multiver")
        Files.move(new File(tempScriptDir,"script-plugin-multiver.zip"),new File(tempManifestDir,"${artifactId}-1.0.0.zip"))
        Thread.sleep(1000)
        TestUtils.setVersion(tempScriptDir.absolutePath+"/script-plugin-multiver/plugin.yaml","1.1")
        TestUtils.zipDir(tempScriptDir.absolutePath+"/script-plugin-multiver")
        Files.move(new File(tempScriptDir,"script-plugin-multiver.zip"),new File(tempManifestDir,"${artifactId}-1.1.zip"))
        generator.generate("Other Artifact", PluginType.script, "WorkflowNodeStep", tempScriptDir.absolutePath)
        Files.move(new File(tempScriptDir,"other-artifact/plugin.yaml"),new File(tempManifestDir,"plugin.yaml"))


        when:
        FilesystemManifestCreator creator = new FilesystemManifestCreator(tempManifestDir.absolutePath)
        ArtifactManifest manifest = creator.createManifest()
        ManifestEntry multiVer = manifest.entries.find { it.id == artifactId}

        then:
        manifest.entries.size() == 2
        multiVer.currentVersion == "1.1"
        multiVer.oldVersions == ["1.0.0"]


    }


}
