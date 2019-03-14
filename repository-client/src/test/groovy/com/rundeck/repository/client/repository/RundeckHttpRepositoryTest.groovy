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
package com.rundeck.repository.client.repository

import com.rundeck.repository.client.RundeckRepositoryClient
import com.rundeck.repository.definition.RepositoryDefinition
import com.rundeck.repository.manifest.ManifestEntry
import com.rundeck.repository.manifest.ManifestService
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.bouncycastle.jce.provider.BouncyCastleProvider
import spock.lang.Specification

import java.security.Security


class RundeckHttpRepositoryTest extends Specification {
    def setup() {
        Security.addProvider(new BouncyCastleProvider());
    }

    def "Ensure Public Key Caching"() {
        setup:
        MockWebServer httpServer = new MockWebServer()
        httpServer.start()
        httpServer.enqueue(new MockResponse().setResponseCode(200).setBody(getClass().getClassLoader().getResourceAsStream("gpg/pubkey.key").text))
        String endpoint = httpServer.url("repo/v1/oss").toString()

        when:
        println endpoint
        RepositoryDefinition repoDef = new RepositoryDefinition()
        repoDef.repositoryName = "OSS"
        repoDef.configProperties.rundeckRepoEndpoint = endpoint
        RundeckHttpRepository repo = new RundeckHttpRepository(repoDef)
        def pubKey = repo.getRundeckPublicKey()
        RecordedRequest r = httpServer.takeRequest()
        def pubKeyFromCache = repo.getRundeckPublicKey()

        then:
        r.path == "/repo/v1/oss/verification-key"
        pubKey
        pubKeyFromCache
        pubKey != pubKeyFromCache
        pubKey.text == pubKeyFromCache.text

    }

    //def "Attempt to download but no binary url"() {

    //}

//    def "Download and verify test"() {
//        when:
//        ManifestService manifestSvc = Stub(ManifestService) {
//            getEntry() >> { new ManifestEntry(currentVersion: "1.0.2-SNAPSHOT")}
//        }
//        RepositoryDefinition repoDef = new RepositoryDefinition()
//        repoDef.repositoryName = "OSS"
//        repoDef.configProperties.rundeckRepoEndpoint = "https://api-stage.rundeck.com/spark/repo/oss"
//        RundeckHttpRepository repository = new RundeckHttpRepository(repoDef)
//        repository.manifestService = manifestSvc
//        File tmpFile = File.createTempFile("tmp","file")
//        tmpFile << repository.getArtifactBinary("36cc72f36df7","1.0.2-SNAPSHOT")
//        then:
//        noExceptionThrown()
//        tmpFile.size() == 3124821
//
//    }
}
