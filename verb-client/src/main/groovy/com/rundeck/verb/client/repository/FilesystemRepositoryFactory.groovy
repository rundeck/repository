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

import com.dtolabs.rundeck.core.storage.ResourceMeta
import com.rundeck.verb.client.manifest.MemoryManifestService
import com.rundeck.verb.repository.RepositoryDefinition
import com.rundeck.verb.repository.RepositoryFactory
import com.rundeck.verb.repository.VerbArtifactRepository
import org.rundeck.storage.api.Tree
import org.rundeck.storage.data.DataUtil
import org.rundeck.storage.data.file.FileTreeUtil

class FilesystemRepositoryFactory implements RepositoryFactory {
    VerbArtifactRepository createRepository(RepositoryDefinition definition) {
        if(!new File(definition.repositoryLocation.toURI()).exists()) throw new Exception("Repository ${definition.repositoryLocation} does not exist")
        if(!new File(definition.manifestLocation.toURI()).exists()) throw new Exception("Manifest ${definition.manifestLocation} does not exist")
        MemoryManifestService manifestService = new MemoryManifestService(definition.manifestLocation)
        return new StorageTreeVerbArtifactRepository(createTree(definition),definition,manifestService)
    }

    Tree<ResourceMeta> createTree(final RepositoryDefinition repositoryDefinition) {
        FileTreeUtil.forRoot(new File(repositoryDefinition.repositoryLocation.toURI()), DataUtil.contentFactory())
    }
}
