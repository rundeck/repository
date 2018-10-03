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

import com.rundeck.verb.client.repository.tree.NamedTreeProvider
import com.rundeck.verb.client.repository.tree.TreeProvider
import com.rundeck.verb.repository.RepositoryDefinition
import com.rundeck.verb.repository.RepositoryFactory
import com.rundeck.verb.repository.RepositoryOwner
import com.rundeck.verb.repository.RepositoryType
import com.rundeck.verb.repository.VerbArtifactRepository

class VerbRepositoryFactory implements RepositoryFactory {

    TreeProvider treeProvider = new NamedTreeProvider()

    VerbArtifactRepository createRepository(RepositoryDefinition definition) {
        VerbArtifactRepository repository
        if(definition.type == RepositoryType.FILE) {
            repository = new FilesystemArtifactRepository(definition)
        } else if(definition.type == RepositoryType.STORAGE_TREE) {
            repository = new StorageTreeVerbArtifactRepository(treeProvider.getTree(definition.configProperties.treeName),definition)
        } else if(definition.type == RepositoryType.HTTP && definition.owner == RepositoryOwner.RUNDECK) {
            repository = new RundeckVerbRepository(definition)
        } else {
            throw new Exception("Unknown repository type: ${definition.type}")
        }
        return repository
    }
}
