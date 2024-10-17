package com.github.m9w.builtin

import com.github.m9w.ReflectTool
import com.github.m9w.hooks.AfterApiInit
import com.github.m9w.scanner.ClasspathScanner
import com.github.manolo8.darkbot.Main
import com.github.manolo8.darkbot.extensions.features.FeatureDefinition
import com.github.manolo8.darkbot.extensions.features.FeatureRegisterHandler
import com.github.manolo8.darkbot.extensions.features.handlers.FeatureHandler
import java.net.URL
import java.util.stream.Stream

object FeatureHandlerLoader : FeatureHandler<Any>() {
    @AfterApiInit
    fun inject() {
        Main.INSTANCE.pluginAPI.addInstance(this)
        val registry = Main.INSTANCE.pluginAPI.requireInstance(FeatureRegisterHandler::class.java)
        ReflectTool.of(registry).getField<MutableList<FeatureHandler<Any>>>("FEATURE_HANDLERS").add(this)
    }

    val NATIVE = ClasspathScanner.getAllResourcesFromClasspath().asSequence().filter { it.endsWith("features.native") }
        .flatMap { BuiltInPluginLoader::class.java.classLoader.getResources(it).asSequence() }
        .map(URL::openStream)
        .map { it.use { String(it.readAllBytes()) } }
        .flatMap { it.split("\\s".toRegex()) }
        .map(String::trim)
        .filter(String::isNotEmpty)
        .map { Class.forName(it) }.toList()
        .toTypedArray()

    override fun getNativeFeatures(): Array<Class<*>> = NATIVE

    override fun update(features: Stream<FeatureDefinition<Any>>?) {}
}
