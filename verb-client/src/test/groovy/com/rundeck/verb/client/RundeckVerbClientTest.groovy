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
import com.rundeck.verb.client.artifact.RundeckVerbArtifact
import com.rundeck.verb.client.artifact.StorageTreeArtifactInstaller
import com.rundeck.verb.client.repository.VerbRepositoryFactory
import com.rundeck.verb.client.repository.RundeckRepositoryManager
import com.rundeck.verb.client.util.ArtifactUtils
import com.rundeck.verb.client.util.ResourceFactory
import org.rundeck.storage.data.DataUtil
import org.rundeck.storage.data.file.FileTreeUtil
import spock.lang.Specification

import java.util.zip.ZipFile


class RundeckVerbClientTest extends Specification {

    def setupSpec() {
        File repoRoot = new File("/tmp/verb-repo")
        if(repoRoot.exists()) repoRoot.deleteDir()
        repoRoot.mkdirs()
        new File("/tmp/verb-repo/manifest.json") << "{}" //Init empty manifest
    }

    def "Upload Artifact To Repo"() {

        when:
        RundeckVerbClient client = new RundeckVerbClient()
        client.repositoryManager = new RundeckRepositoryManager(new VerbRepositoryFactory())
        client.repositoryManager.setRepositoryDefinitionListDatasourceUrl(getClass().getClassLoader().getResource("repository-definition-list.yaml").toString())

        def response = client.uploadArtifact("private",getClass().getClassLoader().getResourceAsStream("binary-artifacts/SuperNotifier-0.1.0-SNAPSHOT.jar"))
        println response.messages[0].message
        println response.messages[0].code
        then:
        response.batchSucceeded()

    }

    //This test depends upon the upload artifact to repo test running first and putting an artifact in the repo
    def "Install Artifact To Plugin Storage"() {
        setup:
        File pluginRoot = new File("/tmp/verb-plugins")
        if(pluginRoot.exists()) pluginRoot.deleteDir()
        pluginRoot.mkdirs()

        when:
        RundeckVerbClient client = new RundeckVerbClient()
        client.artifactInstaller = new StorageTreeArtifactInstaller(new StorageTreeImpl(FileTreeUtil.forRoot(pluginRoot, new ResourceFactory())))
        VerbRepositoryFactory repositoryFactory = new VerbRepositoryFactory()
        client.repositoryManager = new RundeckRepositoryManager(new VerbRepositoryFactory())
        client.repositoryManager.setRepositoryDefinitionListDatasourceUrl(getClass().getClassLoader().getResource("repository-definition-list.yaml").toString())

        ZipFile bin = new ZipFile(new File(getClass().getClassLoader().getResource("binary-artifacts/SuperNotifier-0.1.0-SNAPSHOT.jar").toURI()))
        RundeckVerbArtifact artifact = ArtifactUtils.createArtifactFromStream(ArtifactUtils.extractArtifactMetaFromZip(bin))
        def response = client.installArtifact("private",artifact.id)

        response.messages.each {
            println "${it.code} : ${it.message}"
        }

        then:
        response.batchSucceeded()

    }

    def "List Artifacts"() {
        given:

        RundeckVerbClient client = new RundeckVerbClient()
        client.repositoryManager = new RundeckRepositoryManager(new VerbRepositoryFactory())
        client.repositoryManager.setRepositoryDefinitionListDatasourceUrl(getClass().getClassLoader().getResource("repository-definition-list.yaml").toString())
        client.syncInstalledManifests()

        when:
        def manifestSearchResults = client.listArtifacts()

        then:
        manifestSearchResults.size() == 1
        manifestSearchResults[0].results.size() == 1

    }

    def "List Artifacts By Repo"() {
        given:

        RundeckVerbClient client = new RundeckVerbClient()
        client.repositoryManager = new RundeckRepositoryManager(new VerbRepositoryFactory())
        client.repositoryManager.setRepositoryDefinitionListDatasourceUrl(getClass().getClassLoader().getResource("repository-definition-list.yaml").toString())
        client.syncInstalledManifests()

        when:
        def manifestSearchResults = client.listArtifacts("private")

        then:
        manifestSearchResults.size() == 1
        manifestSearchResults[0].results.size() == 1

    }
}
