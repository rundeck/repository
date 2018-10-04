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
package com.rundeck.repository.client.yaml

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLParser
import com.github.fge.jackson.JsonLoader
import com.github.fge.jsonschema.core.load.configuration.LoadingConfiguration
import com.github.fge.jsonschema.core.load.uri.URITranslatorConfiguration
import com.github.fge.jsonschema.core.report.ProcessingReport
import com.github.fge.jsonschema.main.JsonSchema
import com.github.fge.jsonschema.main.JsonSchemaFactory


class YamlValidator {
    ObjectMapper mapper = new ObjectMapper()
    private final String schemaNamespace
    private Map<String,String> schemaPathLookupMapper = [:]

    YamlValidator(String schemaNamespace) {
        this.schemaNamespace = schemaNamespace
    }

    public void addSchemaPathLookupMapperEntry(String schemaPath, String mappedToUrlLocation) {
        schemaPathLookupMapper[schemaPath] = mappedToUrlLocation
    }

    ProcessingReport validate(String fileToValidate, String validationSchemaFile) {
        validate(new File(fileToValidate).newInputStream(),new FileReader(validationSchemaFile))
    }

    ProcessingReport validate(InputStream toValidate, Reader schemaSource) {
        def translatorConfigurationBuilder = URITranslatorConfiguration.newBuilder()
        if(schemaNamespace) translatorConfigurationBuilder.setNamespace(schemaNamespace)
        schemaPathLookupMapper.each { schemaPath, urlMapping ->
            translatorConfigurationBuilder.addPathRedirect(schemaPath, urlMapping)
        }

        JsonSchemaFactory schemaFactory = JsonSchemaFactory.byDefault().thaw()
                                                           .setLoadingConfiguration(
                LoadingConfiguration.byDefault().thaw()
                                    .setURITranslatorConfiguration(translatorConfigurationBuilder.freeze())
                                    .freeze()).freeze();

        YAMLFactory yamlFactory = new YAMLFactory()
        YAMLParser parser = yamlFactory.createParser(toValidate)
        JsonSchema schema = schemaFactory.getJsonSchema(JsonLoader.fromReader(schemaSource))
        JsonNode parsedFile = mapper.readTree(parser)
        schema.validate(parsedFile)
    }
}
