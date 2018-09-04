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

import com.rundeck.verb.Constants
import com.rundeck.verb.ResponseBatch
import com.rundeck.verb.ResponseCodes
import com.rundeck.verb.repository.RepositoryDefinition
import com.rundeck.verb.repository.RepositoryOwner
import com.rundeck.verb.repository.RepositoryType
import spock.lang.Specification


class RundeckRepositoryManagerTest extends Specification {
    def "setRepositoryDefinitionListDatasourceUrl"() {
        when:
        RundeckRepositoryManager manager = new RundeckRepositoryManager()
        manager.setRepositoryDefinitionListDatasourceUrl(getClass().getClassLoader().getResource("repository-definition-list.yaml").toString())
        then:
        manager.listRepositories().size() == 1
        manager.listRepositories().contains("private")
    }

    def "AddRepository"() {
        setup:
        File tmpRepoDef = File.createTempFile("tmp","repodefn.yaml")
        tmpRepoDef.createNewFile()
        tmpRepoDef << "---"
        when:
        RundeckRepositoryManager manager = new RundeckRepositoryManager()
        manager.setRepositoryDefinitionListDatasourceUrl(tmpRepoDef.toURL().toString())
        RepositoryDefinition rd = new RepositoryDefinition()
        rd.repositoryName = "MyRepo"
        rd.owner = RepositoryOwner.PRIVATE
        rd.type = RepositoryType.FILE
        rd.configProperties.repositoryLocation = "/tmp/myrepo"
        rd.configProperties.manifestLocation = "/tmp/myrepo-manifest.json"
        manager.addRepository(rd)
        then:
        manager.listRepositories().size() == 1
        manager.listRepositories().contains("MyRepo")
    }

    def "Upload Artifact Fail with invalid repo name"() {
        when:
        RundeckRepositoryManager manager = new RundeckRepositoryManager()
        ResponseBatch response = manager.uploadArtifact("invalid",getClass().getClassLoader().getResourceAsStream(Constants.ARTIFACT_META_FILE_NAME))
        then:
        !response.batchSucceeded()
        response.messages[0].code == ResponseCodes.REPO_DOESNT_EXIST
    }

}
