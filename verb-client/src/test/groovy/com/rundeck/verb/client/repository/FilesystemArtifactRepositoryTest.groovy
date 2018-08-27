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
import com.rundeck.verb.client.manifest.MemoryManifestService
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
    FilesystemArtifactRepository repo

    def setupSpec() {
        repoBase = File.createTempDir()
        println repoBase.absolutePath
        repoManifest = new File(repoBase, "manifest.json")
        repoManifest.createNewFile()
        repoManifest << "{}"
        RepositoryDefinition repoDef = new RepositoryDefinition()
        repoDef.repositoryLocation = repoBase.toURL()
        repoDef.manifestLocation = repoManifest.toURL()
        repoDef.type = RepositoryType.FILE
        repoDef.owner = RepositoryOwner.PRIVATE
        MemoryManifestService fsManifestService = new MemoryManifestService(repoDef.manifestLocation)
        repo = new FilesystemArtifactRepository(repoDef, fsManifestService)
        repo.manifestService.syncManifest()
    }

    def "UploadArtifactMeta"() {
        when:
        ResponseBatch rbatch = repo.uploadArtifact(getClass().getClassLoader().getResourceAsStream("rundeck-verb-artifact.yaml"))
        rbatch.messages.each {
            println "${it.code} : ${it.message}"
        }
        then:
        rbatch.batchSucceeded()
        new File(repoBase,"artifacts/4819d98fea70-0.1.yaml").exists()
    }

    def "UploadArtifactBinary"() {
        when:
        ResponseBatch rbatch = repo.uploadArtifact(getClass().getClassLoader().getResourceAsStream("binary-artifacts/SuperNotifier-0.1.0-SNAPSHOT.jar"))
        rbatch.messages.each {
            println "${it.code} : ${it.message}"
        }
        then:
        rbatch.batchSucceeded()
        new File(repoBase,"artifacts/8fb8b21df658-0.1.yaml").exists()
        new File(repoBase,"binary/8fb8b21df658-0.1.jar").exists()
    }

    def "GetArtifact"() {
        expect:
        repo.getArtifact("4819d98fea70")

    }

    def "GetArtifactBinary"() {
        expect:
        repo.getArtifactBinary("8fb8b21df658")
    }


}
