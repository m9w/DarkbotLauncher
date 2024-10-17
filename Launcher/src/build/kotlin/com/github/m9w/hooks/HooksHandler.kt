package com.github.m9w.hooks

import com.github.m9w.scanner.ClasspathScanner
import java.io.IOException
import java.net.URL
import java.util.stream.Stream
import kotlin.reflect.KCallable
import kotlin.reflect.full.hasAnnotation

object HooksHandler {
    private val hooks: Collection<Pair<Runnable, KCallable<*>>> = getHooks()
    lateinit var args: Array<String>

    fun beforeMain(args: Array<String>) {
        HooksHandler.args = args
        applyHook<BeforeMain>()
    }

    fun beforeApiInit() = applyHook<BeforeApiInit>()

    fun afterApiInit() = applyHook<AfterApiInit>()

    private inline fun <reified T : Annotation> applyHook() =
        hooks.filter { it.second.hasAnnotation<T>() }
             .sortedBy { it.second.hashCode() }
             .map { it.first }
             .forEach { it.run() }

    private fun getHooks(): Collection<Pair<Runnable, KCallable<*>>> {
        try {
            return ClasspathScanner.getAllResourcesFromClasspath().asSequence()
                .filter { it.endsWith("class.inject") }
                .flatMap { HooksHandler::class.java.classLoader.getResources(it).asSequence() }
                .map(URL::openStream)
                .map { it.use { String(it.readAllBytes()) } }
                .flatMap { it.split("\\s".toRegex()) }
                .map(String::trim)
                .filter(String::isNotEmpty)
                .map { Class.forName(it).kotlin }
                .filter { it.objectInstance != null }
                .flatMap { clazz ->
                    clazz.members
                        .filter { it.parameters.size == 1 }
                        .filter { it.parameters[0].type.classifier == clazz }
                        .filter { it.annotations.isNotEmpty() }
                        .map { Runnable { it.call(clazz.objectInstance) } to it }
                }
                .toList()
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }
}