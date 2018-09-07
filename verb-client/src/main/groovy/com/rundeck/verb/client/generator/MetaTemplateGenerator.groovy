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
import com.rundeck.verb.ResponseCodes
import com.rundeck.verb.ResponseMessage
import com.rundeck.verb.client.RundeckVerbClient
import com.rundeck.verb.client.util.ArtifactUtils
import groovy.text.GStringTemplateEngine


class MetaTemplateGenerator implements ArtifactTypeTemplateGenerator {
    private static final String META_TEMPLATE = "templates/meta-plugin/${Constants.ARTIFACT_META_FILE_NAME}"

    GStringTemplateEngine engine = new GStringTemplateEngine()
    private Map templateProperties = new HashMap(RundeckVerbClient.clientProperties)

    @Override
    ResponseBatch createTemplate(final String artifactName, final String providedService, final String destinationDir) {
        ResponseBatch batch = new ResponseBatch()
        try {
            templateProperties["newPluginId"] = ArtifactUtils.archiveNameToId(artifactName)
            templateProperties["pluginName"] = artifactName
            templateProperties["sanitizedPluginName"] = ArtifactUtils.sanitizedPluginName(artifactName)
            templateProperties["providedService"] = providedService
            templateProperties["currentDate"] = new Date().format("MM/dd/yyyy")
            templateProperties["pluginLang"] = "script"
            String destDir = destinationDir + "/" + templateProperties.sanitizedPluginName
            File destDirFile = new File(destDir)
            if (destDirFile.exists()) {
                batch.addMessage(
                        new ResponseMessage(
                                code: ResponseCodes.TEMPLATE_GENERATION_LOCATION_EXISTS,
                                message: "Location already exists. Aborting to prevent overwrite."
                        )
                )
                return batch
            } else {
                destDirFile.mkdirs()
                File metaFile = new File(destDir, Constants.ARTIFACT_META_FILE_NAME)
                FileWriter fileOut = new FileWriter(metaFile)
                engine.createTemplate(getClass().getClassLoader().getResource(META_TEMPLATE)).make(templateProperties).writeTo(fileOut)
                fileOut.flush()
            }
            batch.addMessage(new ResponseMessage(code: ResponseCodes.SUCCESS,message: "Created artifact meta file at: ${destDir}"))
        } catch(Exception ex) {
            ex.printStackTrace()
            batch.addMessage(new ResponseMessage(code: ResponseCodes.TEMPLATE_GENERATION_FAILED, message: ex.message))
        }
        return batch
    }
}
