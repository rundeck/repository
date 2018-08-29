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
import com.rundeck.verb.manifest.search.ManifestSearch
import com.rundeck.verb.manifest.search.ManifestSearchResult


interface RepositoryManager {
    void setRepositoryDefinitionListDatasourceUrl(String urlToRepositoryDefinitionListDatasource)
    void addRepository(RepositoryDefinition repositoryDefinition)
    void syncRepository(String repositoryName)
    void syncRepositories()
    List<String> listRepositories()
    ResponseBatch uploadArtifact(String repositoryName, InputStream artifactInputStream)
    Collection<ManifestSearchResult> searchRepositories(ManifestSearch search)
    ManifestSearchResult searchRepository(String repositoryName, ManifestSearch search)
    Collection<ManifestSearchResult> listArtifacts(Integer offset, Integer max)
    Collection<ManifestSearchResult> listArtifacts(String repoName, Integer offset, Integer max)
    VerbArtifact getArtifact(String repositoryName, String artifactId, String artifactVersion)
    InputStream getArtifactBinary(String repositoryName, String artifactId, String artifactVersion)
}
