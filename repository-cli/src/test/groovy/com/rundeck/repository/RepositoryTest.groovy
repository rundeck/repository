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
package com.rundeck.repository

import com.rundeck.plugin.template.FilesystemArtifactTemplateGenerator
import com.rundeck.plugin.template.PluginType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import spock.lang.Shared
import spock.lang.Specification


class RepositoryTest extends Specification {
    @Shared
    FilesystemArtifactTemplateGenerator generator

    def "Submit"() {
        setup:
        MockWebServer httpServer = new MockWebServer()
        httpServer.start()
        httpServer.enqueue(new MockResponse().setResponseCode(200))
        httpServer.enqueue(new MockResponse().setResponseCode(200))
        File buildDir = File.createTempDir()
        generator = new FilesystemArtifactTemplateGenerator()
        generator.generate("UploadScriptTest", PluginType.script,"FileCopier",buildDir.absolutePath)
        zipScript(buildDir,"uploadscripttest")
        Repository repository = new Repository()
        repository.officialRundeckRepoArtifactSaveUrl = httpServer.url("save")
        repository.officialRundeckRepoSubmissionUrl = httpServer.url("submit")
        Repository.SubmitOpts opts = new Repository.SubmitOpts() {

            @Override
            String getAuthorId() {
                return "sj"
            }

            @Override
            String getAuthorToken() {
                return "acceptme"
            }

            @Override
            String getArtifactFilePath() {
                return new File(buildDir,"uploadscripttest.zip").absolutePath
            }
        }
        expect:
        repository.submit(opts)
        httpServer.takeRequest().path == "/submit/script_plugin/cae0183f23a8/1.0.0"
        httpServer.takeRequest().path == "/save"
    }

    private zipScript(File buildDir, String fileName) {
        File rootDir = new File(buildDir,fileName)
        def zipp = new  ProcessBuilder("zip","-r",rootDir.name+".zip", rootDir.name+"/").directory(rootDir.parentFile).start()
        zipp.waitFor()
    }
 }
