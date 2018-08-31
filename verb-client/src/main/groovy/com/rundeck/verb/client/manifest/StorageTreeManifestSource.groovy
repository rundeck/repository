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

import com.dtolabs.rundeck.core.storage.StorageTree
import com.rundeck.verb.client.util.ArtifactUtils
import com.rundeck.verb.client.util.ResourceFactory
import com.rundeck.verb.manifest.ArtifactManifest
import com.rundeck.verb.manifest.ManifestSource
import org.rundeck.storage.data.DataUtil


class StorageTreeManifestSource implements ManifestSource {
    private StorageTree storageTree
    private String manifestPath
    private static final ResourceFactory resourceFactory = new ResourceFactory()

    StorageTreeManifestSource(final StorageTree storageTree, final String manifestPath) {
        this.storageTree = storageTree
        this.manifestPath = manifestPath
        if(!storageTree.hasResource(manifestPath)) {
            storageTree.createResource(manifestPath, DataUtil.withText("{}",[:],resourceFactory))
        }
    }

    @Override
    ArtifactManifest getManifest() {
        InputStream src = storageTree.getResource(manifestPath).contents.inputStream
        ArtifactManifest manifest = ArtifactUtils.artifactManifestFromJson(src.text)
        src.close()
        return manifest
    }

    @Override
    void saveManifest(final ArtifactManifest manifest) {
        storageTree.updateResource(manifestPath,DataUtil.withText(ArtifactUtils.artifactManifestToJson(manifest),[:],resourceFactory))
    }
}
