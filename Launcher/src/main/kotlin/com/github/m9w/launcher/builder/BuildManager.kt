package com.github.m9w.launcher.builder

import com.github.m9w.launcher.patches.Patch
import com.github.m9w.launcher.patches.PatchManager
import com.github.m9w.launcher.properties.LauncherProperties
import java.io.File


object BuildManager {
    private val javaBin = System.getProperty("java.home") + listOf("", "bin", "").joinToString(File.separator)

    fun build(version: String, patches: List<Patch>) {
        PatchManager.updateSubmodules()
        File("release").mkdirs()
        patches.forEach(PatchManager::applyPatch)
        runGradle(version, "jar")
        patches.reversed().forEach { PatchManager.applyPatch(it, true) }
    }

    fun run(command: List<String>, env: Map<String, String> = emptyMap(), workDir: File = File(".")): String {
        if (LauncherProperties.debug) println(command)
        val out = StringBuilder()
        val processBuildr = ProcessBuilder(command).directory(workDir)
        processBuildr.environment().putAll(env)
        val process = processBuildr.start()
        do {
            val inp = String(process.inputStream.readBytes())
            val err = String(process.errorStream.readBytes())
            out.append(inp).append(err)
            if (LauncherProperties.debug) println(inp)
            System.err.print(err)
        } while (process.isAlive)
        return out.toString()
    }

    private fun runGradle(version: String, args: String): String {
        val command = listOf(
            "${javaBin}java", "-Xmx64m", "-Xms64m",
            "-cp", "gradle/wrapper/gradle-wrapper.jar",
            "org.gradle.wrapper.GradleWrapperMain",
            "--warning-mode", "none",
            "--console", "plain",
            args)
        return run(command, env = mapOf("CUSTOM_VERSION" to version))
    }
}
