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
package com.rundeck.repository.client.util

import com.dtolabs.rundeck.core.plugins.PluginUtils
import com.rundeck.plugin.template.FilesystemArtifactTemplateGenerator
import com.rundeck.plugin.template.PluginType
import com.rundeck.repository.artifact.ArtifactType
import com.rundeck.repository.client.TestUtils
import spock.lang.Shared
import spock.lang.Specification

import java.nio.file.Files
import java.nio.file.StandardCopyOption


class ArtifactUtilsTest extends Specification {

    @Shared
    FilesystemArtifactTemplateGenerator generator

    def "get meta from uploaded file"() {
        setup:
        File buildDir = File.createTempDir()
        generator = new FilesystemArtifactTemplateGenerator()
        generator.generate("MyJavaPlugin", PluginType.java,"Notification",buildDir.absolutePath)
        generator.generate("MyScriptPlugin", PluginType.script,"FileCopier",buildDir.absolutePath)
        generator.generate("ManualZipScriptPlugin", PluginType.script,"FileCopier",buildDir.absolutePath)
        TestUtils.buildGradle(new File(buildDir, "myjavaplugin"))
        TestUtils.gradlePluginZip(new File(buildDir,"myscriptplugin"))
        TestUtils.zipDir(new File(buildDir,"manualzipscriptplugin").absolutePath)


        when:
        def jarMeta = ArtifactUtils.getMetaFromUploadedFile(new File(buildDir,"myjavaplugin/build/libs/myjavaplugin-0.1.0.jar"))
        def scriptMeta = ArtifactUtils.getMetaFromUploadedFile(new File(buildDir,"myscriptplugin/build/libs/myscriptplugin-0.1.0-SNAPSHOT.zip"))
        def manualZipScriptMeta = ArtifactUtils.getMetaFromUploadedFile(new File(buildDir,"manualzipscriptplugin.zip"))

        then:
        jarMeta.name == "MyJavaPlugin"
        jarMeta.artifactType == ArtifactType.JAVA_PLUGIN
        scriptMeta.name == "MyScriptPlugin"
        scriptMeta.artifactType == ArtifactType.SCRIPT_PLUGIN
        scriptMeta.originalFilename == "myscriptplugin-0.1.0-SNAPSHOT.zip"
        manualZipScriptMeta.name == "ManualZipScriptPlugin"
        manualZipScriptMeta.artifactType == ArtifactType.SCRIPT_PLUGIN
        manualZipScriptMeta.originalFilename == "manualzipscriptplugin.zip"

    }

    def "rename script"() {
        setup:
        File buildDir = File.createTempDir()
        generator = new FilesystemArtifactTemplateGenerator()
        generator.generate("MyScriptPlugin", PluginType.script,"FileCopier",buildDir.absolutePath)
        generator.generate("ManualZipScriptPlugin", PluginType.script,"FileCopier",buildDir.absolutePath)
        TestUtils.gradlePluginZip(new File(buildDir,"myscriptplugin"))
        TestUtils.zipDir(new File(buildDir,"manualzipscriptplugin").absolutePath)
        File destFile = File.createTempFile("tmp","script")
        Files.copy(new File(buildDir,"myscriptplugin/build/libs/myscriptplugin-0.1.0-SNAPSHOT.zip").toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
        File destFile2 = File.createTempFile("tmp2","script")
        Files.copy(new File(buildDir,"manualzipscriptplugin.zip").toPath(), destFile2.toPath(), StandardCopyOption.REPLACE_EXISTING)

        expect:
        ArtifactUtils.renameScriptFile(destFile).name == "myscriptplugin-0.1.0-SNAPSHOT.zip"
        ArtifactUtils.renameScriptFile(destFile2).name == "manualzipscriptplugin.zip"

    }

    def "get meta from uploaded legacy 1.2 plugin"() {
        when:
        def meta = ArtifactUtils.getMetaFromUploadedFile(new File(getClass().getClassLoader().getResource("legacy-plugins/src-refresh-plugin-1.2.jar").toURI()))
        then:
        meta.id == PluginUtils.generateShaIdFromName(meta.name)
        meta.name == "Node Refresh Plugin"
        meta.version == "3.0.1-SNAPSHOT"
    }
}
