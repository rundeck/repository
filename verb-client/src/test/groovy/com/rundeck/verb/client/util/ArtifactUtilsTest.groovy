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
package com.rundeck.verb.client.util

import com.rundeck.verb.Constants
import com.rundeck.verb.artifact.ArtifactType
import com.rundeck.verb.client.RundeckVerbClient
import com.rundeck.verb.client.TestUtils
import spock.lang.Specification


class ArtifactUtilsTest extends Specification {
    def "Is Binary Plugin"() {
        setup:
        RundeckVerbClient client = new RundeckVerbClient()
        File buildDir = File.createTempDir()
        client.createArtifactTemplate("ScriptIt", ArtifactType.SCRIPT_PLUGIN, "NodeStep", buildDir.absolutePath)
        TestUtils.zipDir(buildDir.absolutePath+ "/scriptit")

        expect:
        ArtifactUtils.isBinaryPlugin(new File(buildDir,"scriptit.zip"))
    }

    def "Is Binary Plugin false for meta"() {
        setup:
        File metaTmp = File.createTempFile("verb","meta")
        metaTmp.deleteOnExit()
        metaTmp << getClass().getClassLoader().getResourceAsStream(Constants.ARTIFACT_META_FILE_NAME)

        expect:
        !ArtifactUtils.isBinaryPlugin(metaTmp)
    }

    def "Extract or set file as meta - meta"() {
        setup:
        File uploadedTmp = File.createTempFile("verb","meta")
        uploadedTmp.deleteOnExit()
        uploadedTmp << getClass().getClassLoader().getResourceAsStream(Constants.ARTIFACT_META_FILE_NAME)

        when:
        File dfile = ArtifactUtils.getMetaFromUploadedFile(uploadedTmp)

        then:
        dfile == uploadedTmp
    }
}
