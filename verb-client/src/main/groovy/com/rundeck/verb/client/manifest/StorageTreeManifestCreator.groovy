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

import com.dtolabs.rundeck.core.storage.ResourceMeta
import com.dtolabs.rundeck.core.storage.StorageUtil
import com.rundeck.verb.client.artifact.RundeckVerbArtifact
import com.rundeck.verb.client.util.ArtifactUtils
import com.rundeck.verb.manifest.ArtifactManifest
import com.rundeck.verb.manifest.ManifestCreator
import com.rundeck.verb.manifest.ManifestEntry
import org.rundeck.storage.api.Tree
import org.rundeck.storage.data.DataContent

class StorageTreeManifestCreator implements ManifestCreator {

    private final Tree<DataContent> repoTree

    StorageTreeManifestCreator(Tree<DataContent> repoTree) {
        this.repoTree = repoTree
    }

    @Override
    ArtifactManifest createManifest() {
        ArtifactManifest manifest = new ArtifactManifest()
        repoTree.listDirectoryResources("artifacts").each { resource ->
            RundeckVerbArtifact artifact = ArtifactUtils.createArtifactFromStream(resource.contents.inputStream)
            ManifestEntry entry = artifact.createManifestEntry()
            entry.lastRelease = resource.contents.meta.get(StorageUtil.RES_META_RUNDECK_CONTENT_CREATION_TIME)
            manifest.entries.add(entry)
        }

        return manifest
    }
}
