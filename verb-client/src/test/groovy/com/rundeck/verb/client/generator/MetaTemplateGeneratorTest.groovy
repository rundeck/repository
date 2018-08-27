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

import spock.lang.Specification


class MetaTemplateGeneratorTest extends Specification {
    def "Create Meta Template"() {
        setup:
        File destDir = File.createTempDir()
        println destDir.absolutePath

        when:
        MetaTemplateGenerator gen = new MetaTemplateGenerator()
        def rbatch = gen.createTemplate("My Artifact","Notification",destDir.absolutePath)

        then:
        rbatch.batchSucceeded()
        new File(destDir,"my-artifact/rundeck-verb-artifact.yaml").exists()

    }
}