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

import com.rundeck.verb.Constants
import com.rundeck.verb.ResponseBatch
import com.rundeck.verb.ResponseCodes
import com.rundeck.verb.ResponseMessage
import com.rundeck.verb.artifact.ArtifactInstaller
import com.rundeck.verb.artifact.VerbArtifact
import com.rundeck.verb.manifest.search.ManifestSearch
import com.rundeck.verb.manifest.search.ManifestSearchResult
import com.rundeck.verb.repository.RepositoryManager

class RundeckVerbClient implements VerbClient {

    RepositoryManager repositoryManager
    ArtifactInstaller artifactInstaller

    @Override
    ResponseBatch saveNewArtifact(final String repositoryName, final VerbArtifact verbArtifact) {
        return repositoryManager.saveNewArtifact(repositoryName,verbArtifact)
    }

    @Override
    ResponseBatch uploadArtifact(final String repositoryName, final InputStream artifactBinaryStream) {
        repositoryManager.uploadArtifact(repositoryName, artifactBinaryStream)
    }

    @Override
    ResponseBatch installArtifact(final String repositoryName, final String artifactId, final String version = null) {
        ResponseBatch response = new ResponseBatch()
        try {
            VerbArtifact artifact = getArtifact(repositoryName, artifactId,version)
            InputStream binarySourceInStream
            if (artifact.binaryLink) {
                URL binaryLink = new URL(artifact.binaryLink)
                //TODO: more sophistication needed here
                binarySourceInStream = binaryLink.openStream()
            } else {
                binarySourceInStream = repositoryManager.getArtifactBinary(repositoryName,artifactId,version)
            }
            response.messages.addAll(artifactInstaller.installArtifact(artifact,binarySourceInStream).messages)
        } catch(Exception ex) {
            response.addMessage(new ResponseMessage(code: ResponseCodes.INSTALL_FAILED,message:ex))
        }
        return response
    }

    @Override
    void syncInstalledManifests() {
        repositoryManager.syncRepositories()
    }

    @Override
    void refreshRepositoryManifest(final String repositoryName) {
        if(repositoryName) repositoryManager.refreshRepositoryManifest(repositoryName)
        else repositoryManager.refreshRepositoryManifest()
    }

    @Override
    Collection<ManifestSearchResult> searchManifests(final ManifestSearch search) {
        return repositoryManager.searchRepositories(search)
    }

    @Override
    Collection<ManifestSearchResult> listArtifacts(Integer offset = 0, Integer limit = -1) {
        return repositoryManager.listArtifacts(offset,limit)
    }

    @Override
    Collection<ManifestSearchResult> listArtifacts(String repoName, Integer offset = 0, Integer limit = -1) {
        return repositoryManager.listArtifacts(repoName, offset,limit)
    }

    @Override
    VerbArtifact getArtifact(final String repositoryName, final String artifactId, final String artifactVersion) {
        return repositoryManager.getArtifact(repositoryName,artifactId, artifactVersion)
    }

    @Override
    InputStream getArtifactBinary(final String repositoryName, final String artifactId, final String artifactVersion) {
        return repositoryManager.getArtifactBinary(repositoryName,artifactId, artifactVersion)
    }

    private static final Map clientProps
    static Map getClientProperties() { clientProps }

    static {
        clientProps = new HashMap(defaultClientProperties())
        Properties props = new Properties()
        try {
            props.load(new FileReader(Constants.VERB_CLIENT_CONFIG_FILE))
            clientProps.putAll(props)
        } catch(Exception ex) {
            //log.warn("unable to load rundeck network properties, using defaults")
        }
    }

    private static Map defaultClientProperties() {
        [
                (RundeckVerbConfigurationProperties.RUNDECK_VERSION):"3.0.x",
                (RundeckVerbConfigurationProperties.AUTHOR): "FirstName LastName",
                (RundeckVerbConfigurationProperties.AUTHOR_ID): "rundeck-verb-userid",
                (RundeckVerbConfigurationProperties.DEV_BASE): System.getProperty("user.home")+"/rundeck-verb-dev"
        ]
    }
}
