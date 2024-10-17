package com.github.m9w.scanner

import java.io.File
import java.net.URL
import java.net.URLDecoder
import java.util.jar.JarFile

object ClasspathScanner {
    private val classLoader = ClasspathScanner::class.java.classLoader

    fun getAllResourcesFromClasspath(): List<String> {
        return classLoader.getResources("META-INF/").asSequence()
            .filter { it.protocol == "jar" }
            .map { it.path.substringAfter("file:").substringBefore("!") }
            .distinct()
            .map { URLDecoder.decode(it, Charsets.UTF_8) }
            .map (::JarFile)
            .flatMap { it.entries().asSequence() }
            .filter { !it.isDirectory }
            .map { it.name }
            .toList() +
                classLoader.getResources("")
            .asSequence()
            .map(URL::toURI)
            .map(::File)
            .flatMap { jar -> jar.walkTopDown().filter { it.isFile }.map { it.toRelativeString(jar) } }
            .map { it.substring(2) }
            .toList()
    }
}