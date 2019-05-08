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
package com.rundeck.repository.client

import com.dtolabs.rundeck.core.storage.StorageTreeImpl
import com.rundeck.plugin.template.FilesystemArtifactTemplateGenerator
import com.rundeck.plugin.template.PluginType
import com.rundeck.repository.client.artifact.RundeckRepositoryArtifact
import com.rundeck.repository.client.artifact.StorageTreeArtifactInstaller
import com.rundeck.repository.client.repository.RundeckRepositoryFactory
import com.rundeck.repository.client.repository.RundeckRepositoryManager
import com.rundeck.repository.client.util.ArtifactUtils
import com.rundeck.repository.client.util.ResourceFactory
import org.rundeck.storage.data.file.FileTreeUtil
import spock.lang.Shared
import spock.lang.Specification

class RundeckRepositoryClientTest extends Specification {

    @Shared
    File repoRoot
    @Shared
    File buildDir
    @Shared
    String builtNotifierPath = "notifier/build/libs/notifier-0.1.0.jar" //assumes buildDir directory
    @Shared
    FilesystemArtifactTemplateGenerator generator

    def setupSpec() {
        buildDir = File.createTempDir()
        println buildDir.absolutePath
        repoRoot = new File("/tmp/repository-repo")
        if(repoRoot.exists()) repoRoot.deleteDir()
        repoRoot.mkdirs()
        new File("/tmp/repository-repo/manifest.json") << "{}" //Init empty manifest
        generator = new FilesystemArtifactTemplateGenerator()
        generator.generate("Notifier", PluginType.java, "Notification", buildDir.absolutePath)
        TestUtils.buildGradle(new File(buildDir,"notifier"))
        generator.generate("ScriptIt", PluginType.script,"NodeExecutor",buildDir.absolutePath)
        TestUtils.zipDir(buildDir.absolutePath+"/scriptit")
        generator.generate("DownloadMe", PluginType.script,"WorkflowNodeStep",buildDir.absolutePath)
    }

    def "Upload Artifact To Repo"() {
        when:
        RundeckRepositoryClient client = new RundeckRepositoryClient()
        client.repositoryManager = new RundeckRepositoryManager(new RundeckRepositoryFactory())
        client.repositoryManager.setRepositoryDefinitionListDatasourceUrl(getClass().getClassLoader().getResource("repository-definition-list.yaml").toString())

        def response = client.uploadArtifact("private",new File(buildDir,builtNotifierPath).newInputStream())
        def response2 = client.uploadArtifact("private",new File(buildDir.absolutePath+"/scriptit.zip").newInputStream())
        def response3 = client.saveNewArtifact("private",ArtifactUtils.createArtifactFromRundeckPluginYaml(new File(buildDir.absolutePath+"/downloadme/plugin.yaml").newInputStream()))

        then:
        response.batchSucceeded()
        response2.batchSucceeded()
        response3.batchSucceeded()

    }

    def "Install Artifact To Plugin Storage"() {
        setup:
        File pluginRoot = new File("/tmp/repository-plugins")
        if(pluginRoot.exists()) pluginRoot.deleteDir()
        pluginRoot.mkdirs()

        when:
        RundeckRepositoryClient client = new RundeckRepositoryClient()
        client.artifactInstaller = new StorageTreeArtifactInstaller(new StorageTreeImpl(FileTreeUtil.forRoot(pluginRoot, new ResourceFactory())),"/")
        client.repositoryManager = new RundeckRepositoryManager(new RundeckRepositoryFactory())
        client.repositoryManager.setRepositoryDefinitionListDatasourceUrl(getClass().getClassLoader().getResource("repository-definition-list.yaml").toString())

        RundeckRepositoryArtifact artifact = ArtifactUtils.getMetaFromUploadedFile(new File(buildDir,builtNotifierPath))
        def response = client.installArtifact("private",artifact.id)

        then:
        response.batchSucceeded()

    }

    def "List Artifacts"() {
        given:

        RundeckRepositoryClient client = new RundeckRepositoryClient()
        client.repositoryManager = new RundeckRepositoryManager(new RundeckRepositoryFactory())
        client.repositoryManager.setRepositoryDefinitionListDatasourceUrl(getClass().getClassLoader().getResource("repository-definition-list.yaml").toString())

        when:
        def manifestSearchResults = client.listArtifacts()

        then:
        manifestSearchResults.size() == 1
        manifestSearchResults[0].results.size() == 3

    }

    def "List Artifacts By Repo"() {
        given:

        RundeckRepositoryClient client = new RundeckRepositoryClient()
        client.repositoryManager = new RundeckRepositoryManager(new RundeckRepositoryFactory())
        client.repositoryManager.setRepositoryDefinitionListDatasourceUrl(getClass().getClassLoader().getResource("repository-definition-list.yaml").toString())

        when:
        def manifestSearchResults = client.listArtifactsByRepository("private")

        then:
        manifestSearchResults.size() == 1
        manifestSearchResults[0].results.size() == 3

    }
}
