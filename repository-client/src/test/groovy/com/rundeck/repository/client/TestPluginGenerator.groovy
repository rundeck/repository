package com.rundeck.repository.client

import groovy.transform.CompileStatic

import java.util.jar.Attributes
import java.util.jar.JarOutputStream
import java.util.jar.Manifest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@CompileStatic
class TestPluginGenerator {

    static def generate(String name, String type, String category, String buildDir) {
        if ("script".equals(type)) {
            generateScriptPlugin(name, category, buildDir)
        } else {
            generateJarPlugin(name, category, buildDir)
        }
    }

    static def generateScriptPlugin(String name, String category, String buildDir) {
        //copy resource path "test-plugins/test-plugin.zip" into the target dir with the given name
        def pluginName = name
        File target = new File(buildDir, "${pluginName}.zip")
        ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(target))
        zipOut.putNextEntry(new ZipEntry(pluginName + "/"))
        zipOut.closeEntry()


        zipOut.putNextEntry(new ZipEntry("${pluginName}/plugin.yaml"))
        String yaml = createPluginYaml(pluginName, category)
        System.err.println("yaml: ${yaml}")
        zipOut << yaml

        zipOut.closeEntry()

        zipOut.putNextEntry(new ZipEntry("${pluginName}/contents/"))
        zipOut.closeEntry()

        zipOut.putNextEntry(new ZipEntry("${pluginName}/contents/script.sh"))
        TestPluginGenerator.classLoader.getResourceAsStream("test-plugins/zip/plugin-test/contents/script.sh").with { input ->
            zipOut << input
        }
        zipOut.closeEntry()
        zipOut.close()
    }

    static String createPluginYaml(String name, String category) {
        def yaml = TestPluginGenerator.classLoader.getResourceAsStream("test-plugins/zip/plugin-test/plugin.yaml").text
        def data = [
                name: name, category: category, title: name
        ]
        yaml = yaml.replaceAll(/\$(\w+?)\$/, { m, key -> data[key] })
        yaml
    }

    static def generateJarPlugin(String name, String category, String buildDir) {
        //copy resource path "test-plugins/test-plugin.zip" into the target dir with the given name
        File target = new File(buildDir, "${name.toLowerCase()}.jar")
//        TestPluginGenerator.classLoader.getResourceAsStream("test-plugins/test-plugin.jar").with { input ->
//            target.withOutputStream { output ->
//                output << input
//            }
//        }
        def manifest = new Manifest()
        manifest.mainAttributes.put(Attributes.Name.MANIFEST_VERSION, "1.0")
        manifest.mainAttributes.put(new Attributes.Name("Rundeck-Plugin-Classnames"), "org.rundeck.plugin.nodes.NodeRefreshWorkflowStep")
        manifest.mainAttributes.put(new Attributes.Name("Rundeck-Plugin-File-Version"), "3.0.1-SNAPSHOT")
        manifest.mainAttributes.put(new Attributes.Name("Rundeck-Plugin-Date"), "2018-05-03T10:58:38-05")
        manifest.mainAttributes.put(new Attributes.Name("Rundeck-Plugin-Description"), "Force refresh node list")
        manifest.mainAttributes.put(new Attributes.Name("Rundeck-Plugin-URL"), "http://rundeck.com")
        manifest.mainAttributes.put(new Attributes.Name("Rundeck-Plugin-Author"), "Rundeck), Inc.")
        manifest.mainAttributes.put(new Attributes.Name("Rundeck-Plugin-Version"), "1.2")
        manifest.mainAttributes.put(new Attributes.Name("Rundeck-Plugin-Name"), name)
        manifest.mainAttributes.put(new Attributes.Name("Rundeck-Plugin-Archive"), "true")

        def stream = new JarOutputStream(new FileOutputStream(target), manifest)
        stream.putNextEntry(new ZipEntry("org/rundeck/plugin/nodes/"))
        stream.closeEntry()

        stream.putNextEntry(new ZipEntry("org/rundeck/plugin/nodes/NodeRefreshWorkflowStep.class"))

        TestPluginGenerator.classLoader.getResourceAsStream("test-plugins/java/org/rundeck/plugin/nodes/NodeRefreshWorkflowStep.class").with { input ->
            stream << input
        }
        stream.closeEntry()
        stream.close()

    }

    static def generatex(String name, String type, String category, String buildDir) {
        //"Notifier", com.rundeck.plugin.template.PluginType.java, "Notification", buildDir.absolutePath
        //create a zip file from the "test-plugins/zip" resources directory
        File resourcesDir = new File("src/test/resources/test-plugins/zip")
        File newFile = new File(buildDir, "${name}.zip")
        ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(newFile))
        resourcesDir.listFiles().each { entry ->
            if (!entry.isDirectory()) {
                def entryName = entry.getName()
                def entryPath = entry.getPath()
                //get relative path from resourcesDir
                entryPath = entryPath.substring(resourcesDir.getPath().length() + 1)
                //write to zip file
                zipOut.putNextEntry(new ZipEntry(entryPath + '/' + entryName))

                def buffer = new byte[entry.size()]
                entry.withInputStream {
                    zipOut.write(buffer, 0, it.read(buffer))
                }
                zipOut.closeEntry()
            }
        }
        zipOut.close()

    }
}
