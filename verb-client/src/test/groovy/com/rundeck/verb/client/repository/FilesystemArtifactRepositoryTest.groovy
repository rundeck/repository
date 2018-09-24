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

import com.dtolabs.rundeck.core.plugins.PluginUtils
import com.google.common.io.Files
import com.rundeck.plugin.template.FilesystemArtifactTemplateGenerator
import com.rundeck.plugin.template.PluginType
import com.rundeck.verb.Constants
import com.rundeck.verb.ResponseBatch
import com.rundeck.verb.artifact.VerbArtifact
import com.rundeck.verb.client.TestUtils
import com.rundeck.verb.client.util.ArtifactUtils
import com.rundeck.verb.repository.RepositoryDefinition
import com.rundeck.verb.repository.RepositoryOwner
import com.rundeck.verb.repository.RepositoryType
import spock.lang.Shared
import spock.lang.Specification


class FilesystemArtifactRepositoryTest extends Specification {

    @Shared
    File repoBase
    @Shared
    File repoManifest
    @Shared
    File buildDir
    @Shared
    String builtNotifierPath = "Notifier/build/libs/Notifier-0.1.0.jar" //assumes buildDir directory
    @Shared
    FilesystemArtifactRepository repo
    @Shared
    FilesystemArtifactTemplateGenerator generator

    def setupSpec() {
        buildDir = File.createTempDir()
        repoBase = File.createTempDir()
        println repoBase.absolutePath
        repoManifest = new File(repoBase, "manifest.json")
        repoManifest.createNewFile()
        repoManifest << "{}"
        RepositoryDefinition repoDef = new RepositoryDefinition()
        repoDef.repositoryName = "private-test"
        repoDef.configProperties.repositoryLocation = repoBase.absolutePath
        repoDef.configProperties.manifestLocation = repoManifest.absolutePath
        repoDef.type = RepositoryType.FILE
        repoDef.owner = RepositoryOwner.PRIVATE
        repo = new FilesystemArtifactRepository(repoDef)
        repo.manifestService.syncManifest()
        generator = new FilesystemArtifactTemplateGenerator()
        generator.generate("Notifier", PluginType.java, "Notification", buildDir.absolutePath)
        TestUtils.buildGradle(new File(buildDir, "Notifier"))
    }

    def "SaveArtifactMetaToRepo"() {
        when:
        VerbArtifact artifact = ArtifactUtils.createArtifactFromYamlStream(getClass().getClassLoader().getResourceAsStream("rundeck-verb-artifact.yaml"))
        ResponseBatch rbatch = repo.saveNewArtifact(artifact)

        then:
        rbatch.batchSucceeded()
        new File(repoBase,"artifacts/4819d98fea70-0.1.yaml").exists()
    }

    def "UploadArtifactBinary"() {
        when:
        ResponseBatch rbatch = repo.uploadArtifact(new File(buildDir,builtNotifierPath).newInputStream())

        then:
        rbatch.batchSucceeded()
        new File(repoBase,"artifacts/882ddccbcdd9-0.1.0.yaml").exists()
        new File(repoBase,"binary/882ddccbcdd9-0.1.0.jar").exists()
    }

    def "GetArtifact"() {
        expect:
        repo.getArtifact("4819d98fea70")

    }

    def "GetArtifactBinary"() {
        expect:
        repo.getArtifactBinary("882ddccbcdd9")
    }

    def "RefreshAndSaveManifest"() {
        when:
        repo.manifestService.listArtifacts().size() == 2
        String pluginName = "ManualManifestTester"
        String manualPlacementPluginId = PluginUtils.generateShaIdFromName(pluginName)
        generator.generate(pluginName, PluginType.script, "NodeExecutor", buildDir.absolutePath)
        Files.copy(
                new File(buildDir, "${pluginName.toLowerCase()}/${Constants.ARTIFACT_META_FILE_NAME}"),
                new File(repoBase, "artifacts/${manualPlacementPluginId}-0.1.yaml")
        )
        repo.recreateAndSaveManifest()

        then:
        repo.manifestService.listArtifacts().size() == 3
        repo.manifestService.listArtifacts().any { it.id == manualPlacementPluginId }

    }

}
