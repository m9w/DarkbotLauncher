package com.github.m9w.launcher.builder

import com.github.m9w.launcher.patches.Patch
import com.github.m9w.launcher.patches.PatchManager
import com.github.m9w.launcher.properties.LauncherProperties
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream


object BuildManager {
    private val javaBin = System.getProperty("java.home") + listOf("", "bin", "").joinToString(File.separator)

    fun build(version: String, patches: List<Patch>) {
        run("git submodule update")
        File("build").mkdirs()
        patches.forEach(PatchManager::applyPatch)
        val apiJars = buildAPI()
        val botJar = buildBot()
        patches.reversed().forEach { PatchManager.applyPatch(it, true) }
        createBuild(apiJars, botJar, version)
    }

    private fun buildAPI(): List<File> {
        val conf = File("DarkBotAPI/build.gradle.kts")
        val originalContent = conf.readText()
        val modifiedContent = originalContent.replace("group = \"eu.darkbot\"", "group = \"eu.darkbot.DarkBotAPI\"")
        conf.writeText(modifiedContent)
        runGradle("DarkBotAPI", "publishToMavenLocal")
        conf.writeText(originalContent)
        return listOf("api", "impl", "shared", "util").map { File("DarkBotAPI/$it/build/libs") }.mapNotNull {
            it.listFiles { _, name -> !name.endsWith("javadoc.jar") && !name.endsWith("sources.jar") }?.first()
        }
    }

    private fun buildBot(): File {
        runGradle("DarkBot", "build")
        return File("DarkBot/build/libs").listFiles { _, name -> !name.endsWith("javadoc.jar") && !name.endsWith("sources.jar") }?.first() ?: throw InternalError("Build not found")
    }

    fun run(command: String, workDir: String? = null): String {
        if (LauncherProperties.debug) println(command)
        val out = StringBuilder()
        val process = Runtime.getRuntime().exec(command, null, workDir?.let(::File))
        do {
            val inp = String(process.inputStream.readBytes())
            val err = String(process.errorStream.readBytes())
            out.append(inp).append(err)
            if (LauncherProperties.debug) print(inp)
            System.err.print(err)
        } while (process.isAlive)
        return out.toString()
    }

    private fun createBuild(apiJars: List<File>, botJar: File, version: String): File {
        val target = File("build/CustomDarkBot-$version.jar")
        ZipOutputStream(FileOutputStream(target)).use { zipOut ->
            zipOut.putNextEntry(ZipEntry("META-INF/MANIFEST.MF"))
            zipOut.writer().apply { write("Manifest-Version: 1.0\r\nMain-Class: Init\r\n\r\n") }.flush()
            zipOut.closeEntry()
            val pathSet = listOf("eu", "com/github/manolo8", "Init", "META-INF")
            ZipFile(botJar).apply {
                entries().asSequence().forEach {
                    if (pathSet.any { p -> it.name.startsWith(p) } && !it.name.startsWith("META-INF"))
                        getInputStream(it).apply {
                            zipOut.putNextEntry(ZipEntry(it.name))
                            copyTo(zipOut)
                            zipOut.closeEntry()
                        }
                }
            }

            val entries = HashSet<String>()
            zipOut.putNextEntry(ZipEntry("dependencies.jar"))
            ZipOutputStream(zipOut).use { zipApiOut ->
                apiJars.map(::ZipFile).forEach { zipApiFile ->
                    zipApiFile.entries().asSequence().filter { entries.add(it.name) }.forEach {
                        zipApiFile.getInputStream(it).apply {
                            zipApiOut.putNextEntry(ZipEntry(it.name))
                            copyTo(zipApiOut)
                            zipApiOut.closeEntry()
                        }
                    }
                }

                ZipFile(botJar).apply {
                    entries().asSequence().forEach {
                        if ( pathSet.none { p -> it.name.startsWith(p) })
                            getInputStream(it).apply {
                                zipApiOut.putNextEntry(ZipEntry(it.name))
                                copyTo(zipApiOut)
                                zipApiOut.closeEntry()
                        }
                    }
                }
            }
        }
        return target
    }

    private fun runGradle(path: String, args: String): String {
        val absPath = File("$path/gradle/wrapper/gradle-wrapper.jar").absolutePath
        return run("${javaBin}java -Xmx64m -Xms64m -cp \"$absPath\" org.gradle.wrapper.GradleWrapperMain --console plain $args", path)
    }
}