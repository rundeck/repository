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
import org.apache.commons.lang.WordUtils

class JavaPluginTemplateGenerator implements ArtifactTypeTemplateGenerator {
    private static final String TEMPLATE_BASE = "templates/java-plugin/"
    private static final String JAVA_STRUCTURE = TEMPLATE_BASE + "java-plugin.structure"

    GStringTemplateEngine engine = new GStringTemplateEngine()
    private Map templateProperties = new HashMap(RundeckVerbClient.clientProperties)

    @Override
    ResponseBatch createTemplate(final String artifactName, final String providedService, final String destinationDir) {
        ResponseBatch batch = new ResponseBatch()
        try {
            templateProperties["newPluginId"] = ArtifactUtils.archiveNameToId(artifactName)
            templateProperties["pluginName"] = artifactName
            templateProperties["sanitizedPluginName"] = sanitzedPluginName(artifactName)
            templateProperties["javaPluginClass"] = validJavaPluginClassFromName(artifactName)
            templateProperties["providedService"] = providedService
            templateProperties["pluginLang"] = "java"
            String destDir = destinationDir + "/" + templateProperties.javaPluginClass
            File destDirFile = new File(destDir)
            if(destDirFile.exists()) {
                batch.addMessage(new ResponseMessage(code:ResponseCodes.TEMPLATE_GENERATION_LOCATION_EXISTS,message: "Location already exists. Aborting to prevent overwrite."))
                return batch
            } else {
                destDirFile.mkdirs()
            }
            StringWriter structureWriter = new StringWriter()
            engine.createTemplate(getClass().getClassLoader().getResource(JAVA_STRUCTURE)).
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
                engine.createTemplate(template).make(templateProperties).writeTo(fileOut)
                fileOut.flush()
            }
            batch.addMessage(new ResponseMessage(code: ResponseCodes.SUCCESS,message: "Created plugin at: ${destDir}"))
        } catch(Exception ex) {
            ex.printStackTrace()
            batch.addMessage(new ResponseMessage(code: ResponseCodes.TEMPLATE_GENERATION_FAILED, message: ex.message))
        }
        return batch
    }

    private def sanitzedPluginName(final String artifactName) {
        return artifactName.replace(" ", "-").replaceAll("[^a-zA-Z]","").toLowerCase()
    }

    private def validJavaPluginClassFromName(final String artifactName) {
        return WordUtils.capitalizeFully(artifactName.replaceAll("[^a-zA-Z\\s]","")).replace(" ","")
    }

}
