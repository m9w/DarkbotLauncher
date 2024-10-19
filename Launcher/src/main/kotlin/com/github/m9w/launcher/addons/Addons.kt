package com.github.m9w.launcher.addons

import java.io.File
import java.net.URI
import java.net.URLDecoder
import java.util.jar.JarFile
import kotlin.reflect.full.isSubclassOf

object Addons {
    private val dir = File("addons")
    private var classLoader = AddonClassLoader(getAddons())
    private val entryPoints = HashSet<EntryPoint>()

    init {
        if (!dir.isDirectory) dir.mkdirs()
        init()
    }

    private fun getAddons() = (dir.listFiles { _, s -> s.endsWith(".jar")}?.map(File::toURI)?.map(URI::toURL) ?: emptyList()).toTypedArray()

    private fun init() {
        for (entryPoint in getEntryPoints()) {
            try {
                val c = classLoader.loadClass(entryPoint).kotlin
                if (c.isSubclassOf(EntryPoint::class)) c.objectInstance?.let { entryPoints.add(it as EntryPoint) }
            } catch (t: Throwable) {
                t.printStackTrace()
            }
        }
        entryPoints.forEach {
            try {
                it.init()
            } catch (t: Throwable) {
                t.printStackTrace()
            }
        }
    }

    private fun close() {
        entryPoints.forEach {
            try {
                it.final()
            } catch (t: Throwable) {
                t.printStackTrace()
            }
        }
        classLoader.close()
        System.gc()
    }

    fun reload(afterClose: Runnable = Runnable {}) {
        close()
        try {
            afterClose.run()
        } catch (t: Throwable) {
            t.printStackTrace()
        }
        classLoader = AddonClassLoader(getAddons())
        init()
    }

    private fun getEntryPoints(): List<String> = getAddons().asSequence()
        .filter { it.protocol == "jar" }
        .map { it.path.substringAfter("file:").substringBefore("!") }
        .distinct()
        .map { URLDecoder.decode(it, Charsets.UTF_8) }
        .map (::JarFile)
        .flatMap { it.entries().asSequence() }
        .filter { !it.isDirectory }
        .map { it.name }
        .filter { it.endsWith(".entrypoint") }
        .flatMap { it.split("\\s".toRegex())}
        .map(String::trim)
        .filter(String::isNotEmpty)
        .distinct()
        .toList()

}
