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
import com.rundeck.verb.artifact.ArtifactType
import com.rundeck.verb.artifact.SupportLevel
import com.rundeck.verb.client.manifest.search.CollectionContainsValueMatchChecker
import com.rundeck.verb.client.manifest.search.CollectionSearchTerm
import com.rundeck.verb.client.manifest.search.ManifestSearchImpl
import com.rundeck.verb.client.manifest.search.StringSearchTerm
import com.rundeck.verb.manifest.ArtifactManifest
import com.rundeck.verb.manifest.ManifestEntry
import spock.lang.Specification


class MemoryManifestServiceTest extends Specification {

    MemoryManifestService memoryManifest

    def setup() {
        memoryManifest = new MemoryManifestService(new FilesystemManifestSource(new File(getClass().getClassLoader().getResource("memory-manifest-service-test.manifest").toURI()).absolutePath))
        memoryManifest.syncManifest()
    }

    def "ListArtifacts"() {
        expect:
        memoryManifest.listArtifacts().size() == 8
    }

    def "ListArtifacts limited"() {
        when:
        def results = memoryManifest.listArtifacts(2,2)
        then:
        results.size() == 2
        results[0].name == "Git Plugin"
    }

    def "SearchArtifacts - Name Match"() {
        when:
        ManifestSearchImpl search = new ManifestSearchImpl()
        search.addSearchTerms(new StringSearchTerm(attributeName: "name", searchValue: "Git Plugin"))
        def results = memoryManifest.searchArtifacts(search)

        then:
        results.size() == 1
        results[0].name == "Git Plugin"
    }

    def "SearchArtifacts - Tag Match"() {
        when:
        ManifestSearchImpl search1 = new ManifestSearchImpl()
        search1.addSearchTerms(new CollectionSearchTerm(attributeName: "tags", searchValue: ["rundeck"], matchChecker: new CollectionContainsValueMatchChecker()))
        def results1 = memoryManifest.searchArtifacts(search1)
        ManifestSearchImpl search2 = new ManifestSearchImpl()
        search2.addSearchTerms(new CollectionSearchTerm(attributeName: "tags", searchValue: ["bash", "script"], matchChecker: new CollectionContainsValueMatchChecker()))
        def results2 = memoryManifest.searchArtifacts(search2)

        then:
        results1.size() == 3
        results1.any { it.name == "Git Plugin" }
        results1.any { it.name == "Script Plugin"}
        results1.any { it.name == "Copy File Plugin"}

        results2.size() == 3
        results2.any { it.name == "Javascript Runner"}
        results2.any { it.name == "Bash It"}
        results2.any { it.name == "Script Plugin"}
    }

    private ManifestEntry createEntry(String name, Map artifactProps = [:] ) {
        Map props = [:]
        props.id = UUID.randomUUID().toString()
        props.name = name
        props.description = "Rundeck plugin"
        props.artifactType = ArtifactType.JAVA_PLUGIN
        props.author = "rundeck"
        props.version = "1.0"
        props.support = SupportLevel.RUNDECK
        props.tags = ["rundeck","orignal"]
        props.putAll(artifactProps)
        return new ManifestEntry(props)
    }

    private void recreateTestData() {
        ArtifactManifest manifest = new ArtifactManifest()
        manifest.entries.add(createEntry("Script Plugin",[tags:["rundeck","bash","script"]]))
        manifest.entries.add(createEntry("Copy File Plugin"))
        manifest.entries.add(createEntry("Git Plugin"))
        manifest.entries.add(createEntry("Super Notifier",["description":"10 different methods to notify job status","author":"Know Itall","support":"COMMUNITY","tags":["notification"]]))
        manifest.entries.add(createEntry("Log Enhancer",["description":"Put anything in your output logs","author":"Log Master","support":"COMMUNITY","tags":["log writer"]]))
        manifest.entries.add(createEntry("Humorous",["description":"Inject today's xkcd commic into your project page","author":"Funny Man","support":"COMMUNITY","artifactType":"UI_PLUGIN","tags":["ui"]]))
        manifest.entries.add(createEntry("Bash It",["description":"Enhance your job with bash","author":"Bash Commander","support":"COMMUNITY","artifactType":"ZIP_PLUGIN","tags":["bash"]]))
        manifest.entries.add(createEntry("Javascript Runner",["description":"Write javascript to do your work","author":"JS Master","support":"COMMUNITY","artifactType":"ZIP_PLUGIN","tags":["js","script"]]))
        new ObjectMapper().writeValue(new File(System.getProperty("user.dir")+"/src/test/resources/memory-manifest-service-test.manifest"),manifest)
    }
}
