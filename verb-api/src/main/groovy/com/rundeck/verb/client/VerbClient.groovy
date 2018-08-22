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
import com.rundeck.verb.artifact.ArtifactType
import com.rundeck.verb.artifact.VerbArtifact
import com.rundeck.verb.manifest.search.ManifestSearch
import com.rundeck.verb.manifest.search.ManifestSearchResult

interface VerbClient {

    ResponseBatch createArtifactTemplate(String artifactName, ArtifactType type, String serviceType, String destinationDir)
    /**
     * Upload a new artifact to a repository
     *
     * @param repositoryName
     * @param artifactBinaryStream
     * @return
     */
    ResponseBatch uploadArtifact(String repositoryName, InputStream artifactBinaryStream)
    /**
     * Install the artifact to Rundeck using the configured artifact installer
     * @param artifact
     * @return
     */
    ResponseBatch installArtifact(String repositoryName, String artifactId, String version)
    Collection<ManifestSearchResult> searchManifests(ManifestSearch search)
    Collection<ManifestSearchResult> listArtifacts(int offset, int limit)
    VerbArtifact getArtifact(String repositoryName, String artifactId, String artifactVersion)
    void syncInstalledManifests()

}