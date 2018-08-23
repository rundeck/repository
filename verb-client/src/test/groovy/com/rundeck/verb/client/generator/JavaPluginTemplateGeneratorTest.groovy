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
package com.rundeck.verb.client.generator

import com.rundeck.verb.Constants
import com.rundeck.verb.ResponseBatch
import com.rundeck.verb.client.validators.YamlArtifactMetaValidator
import spock.lang.Specification

class JavaPluginTemplateGeneratorTest extends Specification {
    YamlArtifactMetaValidator validator = new YamlArtifactMetaValidator()

    def "Create Template"() {
        when:
        File tmpDir = File.createTempDir()
        println tmpDir.absolutePath
        JavaPluginTemplateGenerator generator = new JavaPluginTemplateGenerator()
        generator.createTemplate("My Great Plugin","Notification",tmpDir.absolutePath)
        ResponseBatch response = validator.validate(new File(tmpDir, "/MyGreatPlugin/"+Constants.ARTIFACT_META_FILE_NAME).newInputStream())
        response.messages.each { println it.message }

        then:
        new File(tmpDir,"/MyGreatPlugin/build.gradle").exists()
        response.batchSucceeded()

    }

}
