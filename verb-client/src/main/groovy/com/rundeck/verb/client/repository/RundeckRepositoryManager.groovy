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

import com.fasterxml.jackson.core.JsonEncoding
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.rundeck.verb.ResponseBatch
import com.rundeck.verb.ResponseCodes
import com.rundeck.verb.ResponseMessage
import com.rundeck.verb.artifact.VerbArtifact
import com.rundeck.verb.manifest.search.ManifestSearch
import com.rundeck.verb.manifest.search.ManifestSearchResult
import com.rundeck.verb.repository.RepositoryDefinition
import com.rundeck.verb.repository.RepositoryDefinitionList
import com.rundeck.verb.repository.RepositoryFactory
import com.rundeck.verb.repository.RepositoryManager
import com.rundeck.verb.repository.VerbArtifactRepository


class RundeckRepositoryManager implements RepositoryManager {

    private ObjectMapper mapper = new ObjectMapper()
    private YAMLFactory yamlFactory = new YAMLFactory()
    private Map<String, VerbArtifactRepository> repositories = [:]
    private RepositoryDefinitionList repositoryDefinitions
    private URL repositoryDefinitionDatasource
    private RepositoryFactory repositoryFactory

    RundeckRepositoryManager() {
        this(new VerbRepositoryFactory())
    }
    RundeckRepositoryManager(RepositoryFactory repositoryFactory) {
        this.repositoryFactory = repositoryFactory
    }

    @Override
    void setRepositoryDefinitionListDatasourceUrl(final String urlToRepositoryDefinitionListDatasource) {
        this.repositoryDefinitionDatasource = new URL(urlToRepositoryDefinitionListDatasource)
        repositories.clear()
        loadRepositories()
        syncRepositories()
    }

    void loadRepositories() {
        repositoryDefinitions = mapper.readValue(yamlFactory.createParser(repositoryDefinitionDatasource),RepositoryDefinitionList)
        if(!repositoryDefinitions) repositoryDefinitions = new RepositoryDefinitionList()
        repositoryDefinitions.repositories.each {
            initializeRepoFromDefinition(it)
        }
    }

    @Override
    List<String> listRepositories() {
        return repositories.keySet().toList()
    }

    @Override
    void addRepository(final RepositoryDefinition repositoryDefinition) {
        initializeRepoFromDefinition(repositoryDefinition)
        repositoryDefinitions.repositories.add(repositoryDefinition)
        syncRepository(repositoryDefinition.repositoryName)
        saveRepositoryDefinitionList()
    }

    private void saveRepositoryDefinitionList() {
        mapper.writeValue(yamlFactory.createGenerator(new File(repositoryDefinitionDatasource.toURI()),
                                                      JsonEncoding.UTF8), repositoryDefinitions)
    }

    private void initializeRepoFromDefinition(final RepositoryDefinition repositoryDefinition) {
        repositories[repositoryDefinition.repositoryName] = repositoryFactory.createRepository(repositoryDefinition)
    }

    @Override
    void syncRepository(final String repositoryName) {
        if(!repositories.containsKey(repositoryName)) throw new Exception("Repository ${repositoryName} does not exist.")
        repositories[repositoryName].manifestService.syncManifest()
    }

    @Override
    void syncRepositories() {
        //TODO: do this in parallel
        repositories.values().each {
            it.manifestService.syncManifest()
        }
    }

    @Override
    void refreshRepositoryManifest(final String repositoryName) {
        if(!repositories.containsKey(repositoryName)) throw new Exception("Repository ${repositoryName} does not exist.")
        repositories[repositoryName].recreateAndSaveManifest()
    }

    @Override
    void refreshRepositoryManifests() {
        repositories.values().each { it.recreateAndSaveManifest() }
    }

    @Override
    ResponseBatch uploadArtifact(final String repositoryName, final InputStream artifactInputStream) {
        if(!repositories.containsKey(repositoryName)) return new ResponseBatch().withMessage(new ResponseMessage(code: ResponseCodes.REPO_DOESNT_EXIST,message:"Repository ${repositoryName} does not exist"))
        return repositories[repositoryName].uploadArtifact(artifactInputStream)
    }

    @Override
    Collection<ManifestSearchResult> searchRepositories(final ManifestSearch search) {
        def results = []
        repositories.values().each {
            ManifestSearchResult result = new ManifestSearchResult(repositoryName: it.repositoryDefinition.repositoryName)
            result.results = it.manifestService.searchArtifacts(search)
        }
        return results
    }

    @Override
    ManifestSearchResult searchRepository(final String repositoryName, final ManifestSearch search) {
        if(!repositories.containsKey(repositoryName)) throw new Exception("Repository ${repositoryName} does not exist.")
        ManifestSearchResult result = new ManifestSearchResult(repositoryName:repositoryName)
        result.results = repositories[repositoryName].manifestService.searchArtifacts(search)
        return result
    }

    @Override
    Collection<ManifestSearchResult> listArtifacts(final Integer offset, final Integer max) {
        def results = []
        repositories.values().each {
            ManifestSearchResult result = new ManifestSearchResult(repositoryName: it.repositoryDefinition.repositoryName)
            result.results = it.manifestService.listArtifacts(offset,max)
            results.add(result)
        }
        return results
    }

    @Override
    Collection<ManifestSearchResult> listArtifacts(String repoName, final Integer offset, final Integer max) {
        if(!repositories.containsKey(repoName)) throw new Exception("Repository ${repoName} does not exist.")
        def results = []
        ManifestSearchResult result = new ManifestSearchResult(repositoryName: repoName)
        result.results = repositories[repoName].manifestService.listArtifacts(offset,max)
        results.add(result)
        return results
    }

    @Override
    VerbArtifact getArtifact(final String repositoryName, final String artifactId, final String artifactVersion = null) {
        if(!repositories.containsKey(repositoryName)) throw new Exception("Repository ${repositoryName} does not exist.")
        return repositories[repositoryName].getArtifact(artifactId,artifactVersion)
    }

    @Override
    InputStream getArtifactBinary(final String repositoryName, final String artifactId, final String artifactVersion = null) {
        if(!repositories.containsKey(repositoryName)) throw new Exception("Repository ${repositoryName} does not exist.")
        return repositories[repositoryName].getArtifactBinary(artifactId,artifactVersion)
    }
}
