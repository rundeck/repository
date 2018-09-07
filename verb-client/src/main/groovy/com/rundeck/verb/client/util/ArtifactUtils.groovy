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

import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLParser
import com.rundeck.verb.Constants
import com.rundeck.verb.artifact.ArtifactType
import com.rundeck.verb.artifact.SupportLevel
import com.rundeck.verb.artifact.VerbArtifact
import com.rundeck.verb.client.artifact.RundeckVerbArtifact
import com.rundeck.verb.manifest.ArtifactManifest
import org.apache.commons.codec.digest.DigestUtils
import org.codehaus.groovy.runtime.NullObject

import java.security.MessageDigest
import java.util.jar.JarException
import java.util.jar.JarFile
import java.util.jar.Manifest
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipFile


class ArtifactUtils {
    static HEX = '0123456789abcdef'
    private static ObjectMapper mapper = new ObjectMapper()
    static  {
        mapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
    }

    static ArtifactFileset constructArtifactFileset(InputStream artifactStream) {
        ArtifactFileset fileset = new ArtifactFileset()
        File uploadTmp = File.createTempFile("tmp","artifact")
        uploadTmp << artifactStream
        if(isBinaryPlugin(uploadTmp)) {
            fileset.artifactBinary = uploadTmp
            fileset.artifactMeta = getMetaFromUploadedFile(uploadTmp)
        } else {
            fileset.artifactMeta = uploadTmp
        }
        fileset.artifact = createArtifactFromStream(fileset.artifactMeta.newInputStream())
        fileset
    }

    static File getMetaFromUploadedFile(final File artifactFile) {
        try {
            ZipFile artifact = new ZipFile(artifactFile)
            File tmp = File.createTempFile("artifact","meta")
            tmp << extractArtifactMetaFromZip(artifact)

            return tmp
        } catch(ZipException zipEx) {
            //if the artifact is not a zip then set the destination to the artifact
            return artifactFile
        }

    }

    static boolean isBinaryPlugin(final File artifactFile) {
        try {
            new ZipFile(artifactFile)
            return true
        } catch(ZipException zex) {
            //not a jar probably
        }
        return false
    }

    static InputStream extractArtifactMetaFromZip(final ZipFile artifactZip) {
        ZipEntry emeta = artifactZip.getEntry(Constants.ARTIFACT_META_FILE_NAME)
        if(!emeta) {
            ZipEntry root = artifactZip.entries().nextElement()
            emeta = artifactZip.getEntry(root.name+Constants.ARTIFACT_META_FILE_NAME)
        }
        artifactZip.getInputStream(emeta)
    }

    static RundeckVerbArtifact createArtifactFromStream(InputStream artifactMetaStream) {
        YAMLFactory yamlFactory = new YAMLFactory()
        YAMLParser parser = yamlFactory.createParser(artifactMetaStream)
        mapper.readValue(parser, RundeckVerbArtifact)
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

    static String archiveNameToId(String archiveName) {
        if(!archiveName || archiveName.empty) return null
        MessageDigest digest = DigestUtils.getSha256Digest()
        digest.update(archiveName.bytes)
        return bytesAsHex(digest.digest()).substring(0,12)
    }

    static String bytesAsHex(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return null
        }

        def result = new StringBuilder()

        bytes.each() {
            result << HEX[(it & 0xF0) >> 4]
            result << HEX[it & 0x0F]
        }
        return result.toString()
    }
}
