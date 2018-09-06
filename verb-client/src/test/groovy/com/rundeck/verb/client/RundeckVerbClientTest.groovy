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
package com.rundeck.verb.client

import com.dtolabs.rundeck.core.storage.StorageTreeImpl
import com.rundeck.verb.artifact.ArtifactType
import com.rundeck.verb.client.artifact.RundeckVerbArtifact
import com.rundeck.verb.client.artifact.StorageTreeArtifactInstaller
import com.rundeck.verb.client.repository.VerbRepositoryFactory
import com.rundeck.verb.client.repository.RundeckRepositoryManager
import com.rundeck.verb.client.util.ArtifactUtils
import com.rundeck.verb.client.util.ResourceFactory
import org.rundeck.storage.data.file.FileTreeUtil
import spock.lang.Shared
import spock.lang.Specification

import java.util.zip.ZipFile


class RundeckVerbClientTest extends Specification {

    @Shared
    File repoRoot
    @Shared
    File buildDir
    @Shared
    String builtNotifierPath = "Notifier/build/libs/Notifier-0.1.0-SNAPSHOT.jar" //assumes buildDir directory

    def setupSpec() {
        buildDir = File.createTempDir()
        repoRoot = new File("/tmp/verb-repo")
        if(repoRoot.exists()) repoRoot.deleteDir()
        repoRoot.mkdirs()
        new File("/tmp/verb-repo/manifest.json") << "{}" //Init empty manifest
        RundeckVerbClient client = new RundeckVerbClient()
        client.createArtifactTemplate("Notifier", ArtifactType.JAVA_PLUGIN,"Notification",buildDir.absolutePath)
        TestUtils.buildGradle(new File(buildDir,"Notifier"))
        client.createArtifactTemplate("ScriptIt", ArtifactType.SCRIPT_PLUGIN,"NodeExecutor",buildDir.absolutePath)
        TestUtils.zipDir(buildDir.absolutePath+"/scriptit")
        client.createArtifactTemplate("DownloadMe", ArtifactType.META,"NodeStep",buildDir.absolutePath)
    }

    def "Upload Artifact To Repo"() {
        when:
        RundeckVerbClient client = new RundeckVerbClient()
        client.repositoryManager = new RundeckRepositoryManager(new VerbRepositoryFactory())
        client.repositoryManager.setRepositoryDefinitionListDatasourceUrl(getClass().getClassLoader().getResource("repository-definition-list.yaml").toString())

        def response = client.uploadArtifact("private",new File(buildDir,builtNotifierPath).newInputStream())
        def response2 = client.uploadArtifact("private",new File(buildDir.absolutePath+"/scriptit.zip").newInputStream())
        def response3 = client.uploadArtifact("private",new File(buildDir.absolutePath+"/downloadme/rundeck-verb-artifact.yaml").newInputStream())
        then:
        response.batchSucceeded()
        response2.batchSucceeded()
        response3.batchSucceeded()

    }

    def "Install Artifact To Plugin Storage"() {
        setup:
        File pluginRoot = new File("/tmp/verb-plugins")
        if(pluginRoot.exists()) pluginRoot.deleteDir()
        pluginRoot.mkdirs()

        when:
        RundeckVerbClient client = new RundeckVerbClient()
        client.artifactInstaller = new StorageTreeArtifactInstaller(new StorageTreeImpl(FileTreeUtil.forRoot(pluginRoot, new ResourceFactory())))
        client.repositoryManager = new RundeckRepositoryManager(new VerbRepositoryFactory())
        client.repositoryManager.setRepositoryDefinitionListDatasourceUrl(getClass().getClassLoader().getResource("repository-definition-list.yaml").toString())

        ZipFile bin = new ZipFile(new File(buildDir,builtNotifierPath))
        RundeckVerbArtifact artifact = ArtifactUtils.createArtifactFromStream(ArtifactUtils.extractArtifactMetaFromZip(bin))
        def response = client.installArtifact("private",artifact.id)

        then:
        response.batchSucceeded()

    }

    def "List Artifacts"() {
        given:

        RundeckVerbClient client = new RundeckVerbClient()
        client.repositoryManager = new RundeckRepositoryManager(new VerbRepositoryFactory())
        client.repositoryManager.setRepositoryDefinitionListDatasourceUrl(getClass().getClassLoader().getResource("repository-definition-list.yaml").toString())

        when:
        def manifestSearchResults = client.listArtifacts()

        then:
        manifestSearchResults.size() == 1
        manifestSearchResults[0].results.size() == 3

    }

    def "List Artifacts By Repo"() {
        given:

        RundeckVerbClient client = new RundeckVerbClient()
        client.repositoryManager = new RundeckRepositoryManager(new VerbRepositoryFactory())
        client.repositoryManager.setRepositoryDefinitionListDatasourceUrl(getClass().getClassLoader().getResource("repository-definition-list.yaml").toString())

        when:
        def manifestSearchResults = client.listArtifacts("private")

        then:
        manifestSearchResults.size() == 1
        manifestSearchResults[0].results.size() == 3

    }
}
