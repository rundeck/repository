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
import com.rundeck.verb.artifact.ArtifactType
import com.rundeck.verb.template.ArtifactTemplateGenerator


class FilesystemArtifactTemplateGenerator implements ArtifactTemplateGenerator {
    @Override
    ResponseBatch generate(final String artifactName, final ArtifactType artifactType, final String providedService, final String destinationDir) {
        return getGenerator(artifactType).createTemplate(artifactName, providedService, destinationDir)
    }

    private ArtifactTypeTemplateGenerator getGenerator(ArtifactType artifactType) {
        switch(artifactType) {
            case ArtifactType.JAVA_PLUGIN: return new JavaPluginTemplateGenerator()
            case ArtifactType.SCRIPT_PLUGIN: return new ScriptPluginTemplateGenerator()
            case ArtifactType.META: return new MetaTemplateGenerator()
        }
    }
}
