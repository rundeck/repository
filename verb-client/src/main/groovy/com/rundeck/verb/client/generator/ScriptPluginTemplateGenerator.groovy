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

import com.rundeck.verb.ResponseBatch
import com.rundeck.verb.ResponseCodes
import com.rundeck.verb.ResponseMessage
import com.rundeck.verb.client.RundeckVerbClient
import com.rundeck.verb.client.util.ArtifactUtils
import groovy.text.GStringTemplateEngine


class ScriptPluginTemplateGenerator implements ArtifactTypeTemplateGenerator {
    private static final String TEMPLATE_BASE = "templates/script-plugin/"
    private static final String SCRIPT_STRUCTURE = TEMPLATE_BASE + "script-plugin.structure"

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
            if(destDirFile.exists()) {
                batch.addMessage(new ResponseMessage(code: ResponseCodes.TEMPLATE_GENERATION_LOCATION_EXISTS, message: "Location already exists. Aborting to prevent overwrite."))
                return batch
            } else {
                destDirFile.mkdirs()
            }
            StringWriter structureWriter = new StringWriter()
            engine.createTemplate(getClass().getClassLoader().getResource(SCRIPT_STRUCTURE)).
                    make(templateProperties).
                    writeTo(structureWriter)

            structureWriter.toString().eachLine { entry ->
                if (entry.isEmpty()) return
                def entryParts = entry.split("->").toList()
                String templateName = entryParts.first()
                String destinationName = entryParts.last()
                URL template = getClass().getClassLoader().getResource(TEMPLATE_BASE + templateName)
                def destParts = destinationName.split("/").toList()
                String fileName = destParts.remove(destParts.size() - 1)
                String remainingPath = destParts.join("/")
                if (!remainingPath.isEmpty()) {
                    new File(destDir, remainingPath).mkdirs()
                }
                //println TEMPLATE_BASE+fileName
                File destFile = new File(destDir, remainingPath + "/" + fileName)

                FileWriter fileOut = new FileWriter(destFile)
                if(template.toString().endsWith(".template")) {
                    engine.createTemplate(template).make(templateProperties).writeTo(fileOut)
                    fileOut.flush()
                } else {
                    fileOut << template.openStream()
                }
            }
            batch.addMessage(new ResponseMessage(code: ResponseCodes.SUCCESS,message: "Created plugin at: ${destDir}"))
        } catch(Exception ex) {
            ex.printStackTrace()
            batch.addMessage(new ResponseMessage(code: ResponseCodes.TEMPLATE_GENERATION_FAILED, message: ex.message))
        }
        return batch
    }
}
