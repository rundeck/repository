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
package com.rundeck.verb.client.artifact

import com.dtolabs.rundeck.core.storage.ResourceMeta
import com.dtolabs.rundeck.core.storage.StorageUtil
import com.rundeck.verb.ResponseBatch
import com.rundeck.verb.ResponseCodes
import com.rundeck.verb.ResponseMessage
import com.rundeck.verb.artifact.ArtifactInstaller
import com.rundeck.verb.artifact.VerbArtifact
import org.rundeck.storage.api.Tree
import org.rundeck.storage.data.DataUtil


class StorageTreeArtifactInstaller implements ArtifactInstaller {

    private Tree<ResourceMeta> storageTree

    StorageTreeArtifactInstaller(Tree<ResourceMeta> storageTree) {
        this.storageTree = storageTree
    }

    @Override
    ResponseBatch installArtifact(final VerbArtifact artifact, InputStream binaryInputStream) {
        ResponseBatch batch = new ResponseBatch()
        try {
            String artifactKey = "plugins/"+artifact.id+"."+artifact.artifactType.extension
            def resource = DataUtil.withStream(binaryInputStream, [:], StorageUtil.factory())
            if(storageTree.hasResource(artifactKey)) {
                storageTree.updateResource(artifactKey, resource)
            } else {
                storageTree.createResource(artifactKey, resource)
            }
            try {
                //some input streams must be closed manually or they will
                //retain resources
                binaryInputStream.close()
            } catch(IOException iex) {
                //TODO log.warn(unable to close input stream)
                //probably not a big deal so we don't worry about it
            }
            batch.addMessage(ResponseMessage.success())
        } catch(Exception ex) {
            batch.addMessage(new ResponseMessage(code: ResponseCodes.INSTALL_FAILED,message: ex.message))
        }

        return batch
    }
}
