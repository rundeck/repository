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
package com.rundeck.verb.client.manifest

import com.rundeck.verb.artifact.ArtifactType
import com.rundeck.verb.client.artifact.RundeckVerbArtifact
import com.rundeck.verb.client.util.ArtifactUtils
import com.rundeck.verb.manifest.ArtifactManifest
import com.rundeck.verb.manifest.ManifestCreator

import java.util.zip.ZipFile


class FilesystemManifestCreator implements ManifestCreator {

    private final String artifactScanDir

    FilesystemManifestCreator(String artifactScanDir) {
        this.artifactScanDir = artifactScanDir
    }

    @Override
    ArtifactManifest createManifest() {
        File scanDir = new File(artifactScanDir)
        if(!scanDir.exists() || !scanDir.isDirectory()) {
            throw new FileNotFoundException("${artifactScanDir} does not exists or is not a directory.")
        }
        ArtifactManifest manifest = new ArtifactManifest()
        scanDir.traverse(type: groovy.io.FileType.FILES,nameFilter: ~/.*\.jar|zip|yaml/) { file ->
            ArtifactType guessed = guessArtifactType(file)
            InputStream metaInStream
            if(guessed == ArtifactType.META) {
                metaInStream = file.newInputStream()
            } else {
                println file.absolutePath
                metaInStream = ArtifactUtils.extractArtifactMetaFromZip(new ZipFile(file))
            }
            RundeckVerbArtifact artifact = ArtifactUtils.createArtifactFromStream(metaInStream)
            manifest.entries.add(artifact.createManifestEntry())
        }
        return manifest
    }

    private ArtifactType guessArtifactType(final File file) {
        if(file.name.endsWith("jar")) return ArtifactType.JAVA_PLUGIN
        if(file.name.endsWith("zip")) return ArtifactType.ZIP_PLUGIN
        return ArtifactType.META
    }
}
