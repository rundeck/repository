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
package com.rundeck.verb.repository

import com.rundeck.verb.ResponseBatch
import com.rundeck.verb.artifact.VerbArtifact
import com.rundeck.verb.manifest.ManifestService


interface VerbArtifactRepository {
    RepositoryDefinition getRepositoryDefinition()
    VerbArtifact getArtifact(String artifactId, String version)
    InputStream getArtifactBinary(String artifactId, String version)
    ResponseBatch saveNewArtifact(VerbArtifact verbArtifact)
    ResponseBatch uploadArtifact(InputStream artifactInputStream)
    ManifestService getManifestService()
    void recreateAndSaveManifest()
}