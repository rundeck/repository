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

import com.rundeck.verb.ResponseBatch
import com.rundeck.verb.artifact.VerbArtifact
import com.rundeck.verb.client.manifest.MemoryManifestService
import com.rundeck.verb.client.manifest.MemoryManifestSource
import com.rundeck.verb.manifest.ManifestService
import com.rundeck.verb.repository.RepositoryDefinition
import com.rundeck.verb.repository.VerbArtifactRepository


class TestArtifactRepository implements VerbArtifactRepository {
    MemoryManifestService service = new MemoryManifestService(new MemoryManifestSource())
    RepositoryDefinition repoDefn

    TestArtifactRepository(MemoryManifestService service, RepositoryDefinition repDefn) {
        this.service = service
        this.repoDefn = repDefn
        this.service.syncManifest()
    }

    @Override
    RepositoryDefinition getRepositoryDefinition() {
        return repoDefn
    }

    @Override
    VerbArtifact getArtifact(final String artifactId, final String version) {
        return null
    }

    @Override
    InputStream getArtifactBinary(final String artifactId, final String version) {
        return null
    }

    @Override
    ResponseBatch saveNewArtifact(final VerbArtifact verbArtifact) {
        return null
    }

    @Override
    ResponseBatch uploadArtifact(final InputStream artifactInputStream) {
        return null
    }

    @Override
    ManifestService getManifestService() {
        return service
    }

    @Override
    void recreateAndSaveManifest() {

    }
}
