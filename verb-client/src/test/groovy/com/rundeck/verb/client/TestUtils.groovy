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
package com.rundeck.verb.client

import com.rundeck.verb.client.generator.JavaPluginTemplateGenerator

import java.util.zip.ZipFile


class TestUtils {

    static void buildGradle(File baseDir) {
        Process p = new ProcessBuilder("gradle","build").directory(baseDir).start()
        p.waitFor()
    }

    static void copyToTestBinaryArtifactsResourceLocation(String fileSource) {
        File source = new File(fileSource)
        File destDir = new File(System.getProperty("user.dir")+"/src/test/resources/binary-artifacts")
        File destFile = new File(destDir,source.name)

        source.withInputStream {
            destFile << it
        }
    }

    static refreshTestBinaryArtifact() {
        File destDir = File.createTempDir()
        String destDirPath = destDir.absolutePath
        if(!destDir.exists()) destDir.mkdirs()
        JavaPluginTemplateGenerator generator = new JavaPluginTemplateGenerator()
        generator.createTemplate("Super Notifier","Notification",destDir.absolutePath)
        buildGradle(new File(destDir,"SuperNotifier"))
        copyToTestBinaryArtifactsResourceLocation("${destDirPath}/SuperNotifier/build/libs/SuperNotifier-0.1.0-SNAPSHOT.jar")
        println "generated at: ${destDirPath}"
    }

    static void zipDir(final String dirToZip) {
        File rootDir = new File(dirToZip)
        def zipp = new  ProcessBuilder("zip","-r",rootDir.name+".zip", rootDir.name+"/").directory(rootDir.parentFile).start()
        zipp.waitFor()
    }

    static void setVersion(final String sartifactFile, final String newVersion) {
        File artifactFile = new File(sartifactFile)
        List<String> artifactLines = artifactFile.readLines()
        artifactFile.withOutputStream { out ->
            artifactLines.each { line ->
                if(line.startsWith("Version:")) {
                    out << "Version: '${newVersion}'\n"
                } else {
                    out << line + "\n"
                }
            }
        }
    }
}
