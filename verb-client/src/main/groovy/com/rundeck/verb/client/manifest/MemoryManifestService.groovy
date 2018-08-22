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

import com.fasterxml.jackson.databind.ObjectMapper
import com.rundeck.verb.ResponseCodes
import com.rundeck.verb.ResponseMessage
import com.rundeck.verb.artifact.VerbArtifact
import com.rundeck.verb.manifest.ArtifactManifest
import com.rundeck.verb.manifest.ManifestService
import com.rundeck.verb.manifest.ManifestEntry
import com.rundeck.verb.manifest.search.ManifestSearch
import com.rundeck.verb.manifest.search.ManifestSearchResult
import com.rundeck.verb.manifest.search.SearchTerm


class MemoryManifestService implements ManifestService {

    static ObjectMapper jmap = new ObjectMapper()

    private final URL manifestDatasource
    private List<ManifestEntry> artifacts = []

    MemoryManifestService(String manifestDatasourceUrl) {
        this(new URL(manifestDatasourceUrl))
    }

    MemoryManifestService(URL manifestDatasource) {
        this.manifestDatasource = manifestDatasource
    }

    @Override
    Collection<ManifestEntry> listArtifacts(int offset = 0, int max = -1) {
        if(offset == 0 && max == -1) return artifacts.sort(artifactSorter).asImmutable()

        int lmax = offset+ max > artifacts.size()- 1 ? artifacts.size()- 1 : offset+ max
        return artifacts.sort(artifactSorter).subList(offset, lmax).asImmutable()
    }

    private static Closure getArtifactSorter() {
        { a, b -> a.name <=> b.name }
    }

    @Override
    Collection<ManifestEntry> searchArtifacts(final ManifestSearch search) {
        Set<ManifestEntry> searchResuls = [] as Set
        if(!search) return searchResuls
        artifacts.each { artifact ->
            search.listSearchTerms().each { SearchTerm term ->
                if(term.matchChecker.matches(artifact[term.attributeName],term.searchValue)) {
                    searchResuls.add(artifact)
                }
            }
        }

        return searchResuls.sort(artifactSorter)
    }

    @Override
    ResponseMessage syncManifest() {
        try {
            ArtifactManifest newManifest = jmap.readValue(manifestDatasource, ArtifactManifest)
            artifacts.clear()
            newManifest.entries.each { artifacts.add(it) }
            return ResponseMessage.success()
        } catch(Exception ex) {
            ///log ex
            ex.printStackTrace()
            return new ResponseMessage(code:ResponseCodes.MANIFEST_SYNC_FAILURE,message: ex.message)
        }
    }

    @Override
    ManifestEntry getEntry(final String artifactId) {
        return artifacts.find { it.id == artifactId }
    }
}
