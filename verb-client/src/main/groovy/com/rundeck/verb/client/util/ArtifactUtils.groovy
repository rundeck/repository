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

import com.dtolabs.rundeck.core.plugins.JarPluginProviderLoader
import com.dtolabs.rundeck.core.plugins.PluginMetadata
import com.dtolabs.rundeck.core.plugins.ScriptPluginProviderLoader
import com.dtolabs.rundeck.core.plugins.metadata.PluginMeta
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import com.fasterxml.jackson.dataformat.yaml.YAMLParser
import com.rundeck.verb.Constants
import com.rundeck.verb.artifact.ArtifactType
import com.rundeck.verb.artifact.SupportLevel
import com.rundeck.verb.artifact.VerbArtifact
import com.rundeck.verb.client.artifact.RundeckVerbArtifact
import com.rundeck.verb.manifest.ArtifactManifest
import org.apache.commons.codec.digest.DigestUtils
import org.codehaus.groovy.runtime.NullObject

import java.nio.file.CopyOption
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.jar.JarException
import java.util.jar.JarFile
import java.util.jar.Manifest
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipFile


class ArtifactUtils {
    private static ObjectMapper mapper = new ObjectMapper()
    private static File unusedCacheDir = File.createTempDir()
    static  {
        mapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
        mapper.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false)
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    }

    static ArtifactFileset constructArtifactFileset(InputStream artifactStream) {
        ArtifactFileset fileset = new ArtifactFileset()
        File uploadTmp = File.createTempFile("tmp","artifact")
        uploadTmp << artifactStream
        fileset.artifact = getMetaFromUploadedFile(uploadTmp)
        fileset.artifactBinary = uploadTmp
        fileset
    }

    static RundeckVerbArtifact getMetaFromUploadedFile(final File artifactFile) {
        if(!artifactFile.exists()) throw new Exception("Artifact file: ${artifactFile.absolutePath} does not exist!")
        RundeckVerbArtifact artifact = null
        try {
            JarPluginProviderLoader jarLoader = new JarPluginProviderLoader(artifactFile,unusedCacheDir,unusedCacheDir)
            artifact = createArtifactFromPluginMetadata(jarLoader)
            artifact.providesServices = []
            jarLoader.listProviders().each {
                artifact.providesServices.add(it.service)
            }
            return artifact
        } catch(Exception ex) {
            //not a jar try script
        }

        artifact = createArtifactFromRundeckPluginYaml(extractArtifactMetaFromZip(new ZipFile(artifactFile)))
        return artifact
    }

    static File renameScriptFile(final File scriptFile) {
        println scriptFile.absolutePath
        ZipFile zip = new ZipFile(scriptFile)
        ZipEntry root = zip.entries().nextElement()
        File destFile = new File(scriptFile.parentFile,root.getName().replace("/","")+".zip")
        Files.copy(scriptFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
        return destFile
    }

    static InputStream extractArtifactMetaFromZip(final ZipFile artifactZip) {
        ZipEntry emeta = artifactZip.getEntry(Constants.ARTIFACT_META_FILE_NAME)
        if(!emeta) {
            ZipEntry root = artifactZip.entries().nextElement()
            emeta = artifactZip.getEntry(root.name+Constants.ARTIFACT_META_FILE_NAME)
        }
        artifactZip.getInputStream(emeta)
    }

    static RundeckVerbArtifact createArtifactFromPluginMetadata(PluginMetadata pluginMetadata) {
        RundeckVerbArtifact artifact = new RundeckVerbArtifact()
        artifact.id = pluginMetadata.getPluginId()
        artifact.name = pluginMetadata.getPluginName()
        artifact.description = pluginMetadata.getPluginDescription()
        artifact.artifactType = pluginMetadata.getPluginType() == "jar" ? ArtifactType.JAVA_PLUGIN : ArtifactType.SCRIPT_PLUGIN
        artifact.version = pluginMetadata.getPluginFileVersion()
        artifact.author = pluginMetadata.getPluginAuthor()
        artifact.releaseDate = pluginMetadata.getPluginDate()?.time ?: pluginMetadata.getDateLoaded()?.time
        artifact.rundeckCompatibility = pluginMetadata.getRundeckCompatibilityVersion()
        artifact.sourceLink = pluginMetadata.getPluginSourceLink()
        artifact.tags = pluginMetadata.getTags()
        artifact.license = pluginMetadata.getPluginLicense()
        artifact.thirdPartyDependencies = pluginMetadata.pluginThirdPartyDependencies
        artifact.targetHostCompatibility = pluginMetadata.targetHostCompatibility
        return artifact
    }

    static RundeckVerbArtifact createArtifactFromRundeckPluginYaml(InputStream pluginYamlStream) {
        YAMLFactory yamlFactory = new YAMLFactory()
        YAMLParser parser = yamlFactory.createParser(pluginYamlStream)
        def meta = mapper.readValue(parser, PluginMeta)
        def plugin = new PluginMetaToPluginMetadataAdaptor(meta)
        def artifact = createArtifactFromPluginMetadata(plugin)
        artifact.providesServices = []
        plugin.pluginDefs().each {
            artifact.providesServices.add(it.service)
        }
        return artifact
    }

    static RundeckVerbArtifact createArtifactFromYamlStream(InputStream artifactMetaStream) {
        YAMLFactory yamlFactory = new YAMLFactory()
        YAMLParser parser = yamlFactory.createParser(artifactMetaStream)
        mapper.readValue(parser, RundeckVerbArtifact)
    }

    static def saveArtifactToOutputStream(final VerbArtifact verbArtifact, final OutputStream targetStream) {
        YAMLFactory yamlFactory = new YAMLFactory()
        YAMLGenerator generator = yamlFactory.createGenerator(targetStream)
        mapper.writeValue(generator,verbArtifact)
    }

    static void writeArtifactManifestToFile(ArtifactManifest manifest, OutputStream outputStream) {
        mapper.writeValue(outputStream,manifest)
    }

    static String artifactManifestToJson(ArtifactManifest manifest) {
        mapper.writeValueAsString(manifest)
    }

    static ArtifactManifest artifactManifestFromJson(String manifestJson) {
        mapper.readValue(manifestJson, ArtifactManifest)
    }

    static String niceArtifactTypeName(ArtifactType type) {
        type.name().toLowerCase().replace("_","-")
    }

    static ArtifactType artifactTypeFromNice(String niceArtifactTypeName) {
        ArtifactType.valueOf(niceArtifactTypeName.replace("-","_").toUpperCase())
    }

    static String niceSupportLevelName(SupportLevel level) {
        if(!level) return level
        level.name().toLowerCase().replace("_"," ")
    }

    static SupportLevel supportLevelFromNice(String niceSupportLevelName) {
        SupportLevel.valueOf(niceSupportLevelName.toUpperCase().replace(" ","_"))
    }

    static String artifactMetaFileName(final String artifactId, final String artifactVersion) {
        return artifactId+"-"+artifactVersion+".yaml"
    }

    static String artifactBinaryFileName(final String artifactId, final String artifactVersion, final String artifactExtension) {
        return artifactId+"-"+artifactVersion+"."+artifactExtension
    }

    static String sanitizedPluginName(final String artifactName) {
        return artifactName.replace(" ", "-").replaceAll("[^a-zA-Z\\-]","").toLowerCase()
    }
}
