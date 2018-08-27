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
package com.rundeck.verb.client.artifact

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.rundeck.verb.artifact.ArtifactType
import com.rundeck.verb.artifact.VerbArtifact
import com.rundeck.verb.artifact.SupportLevel
import com.rundeck.verb.client.util.ArtifactTypeDeserializer
import com.rundeck.verb.client.util.ArtifactTypeSerializer
import com.rundeck.verb.client.util.ArtifactUtils
import com.rundeck.verb.client.util.SupportLevelTypeDeserializer
import com.rundeck.verb.client.util.SupportLevelTypeSerializer
import com.rundeck.verb.manifest.ManifestEntry


class RundeckVerbArtifact implements VerbArtifact {

    String id
    String name
    String description
    Long releaseDate
    @JsonSerialize(using= ArtifactTypeSerializer)
    @JsonDeserialize(using= ArtifactTypeDeserializer)
    ArtifactType artifactType
    String author
    String authorId
    String version
    String rundeckCompatibility
    @JsonSerialize(using= SupportLevelTypeSerializer)
    @JsonDeserialize(using= SupportLevelTypeDeserializer)
    SupportLevel support
    String license
    Collection<String> tags
    Collection<String> providesServices
    String thirdPartyDependencies
    String sourceLink
    String binaryLink
    String webLink
    String logoLink

    ManifestEntry createManifestEntry() {
        ManifestEntry entry = new ManifestEntry()
        entry.id = id
        entry.name = name
        entry.author = author
        entry.description = description
        entry.support = ArtifactUtils.niceSupportLevelName(support)
        entry.artifactType = ArtifactUtils.niceArtifactTypeName(artifactType)
        entry.currentVersion = version
        entry.rundeckCompatibility = rundeckCompatibility
        entry.providesServices = providesServices
        entry.tags = tags
        entry.lastRelease = releaseDate
        entry.binaryLink = binaryLink
        return entry
    }
}
