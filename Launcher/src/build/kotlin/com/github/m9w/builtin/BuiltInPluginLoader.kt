package com.github.m9w.builtin

import com.github.m9w.hooks.BeforeApiInit
import com.github.m9w.scanner.ClasspathScanner
import com.github.manolo8.darkbot.Main
import com.github.manolo8.darkbot.extensions.plugins.Plugin
import com.github.manolo8.darkbot.extensions.plugins.PluginDefinition
import com.github.manolo8.darkbot.extensions.plugins.PluginHandler
import com.github.manolo8.darkbot.extensions.plugins.PluginListener
import com.google.gson.Gson
import java.io.InputStreamReader

object BuiltInPluginLoader: PluginListener {
    private val GSON = Gson()

    override fun afterLoad() {
        return ClasspathScanner.getAllResourcesFromClasspath().filter { it.endsWith("plugin.json") }
            .flatMap { BuiltInPluginLoader::class.java.classLoader.getResources(it).asSequence() }
            .map { it.openStream() }
            .forEach {
                InputStreamReader(it).use { reader ->
                    val plugin = Plugin(null, null)
                    plugin.definition = GSON.fromJson(reader, PluginDefinition::class.java)
                    Main.INSTANCE.pluginHandler.LOADED_PLUGINS.add(plugin)
                }
            }
    }

    @BeforeApiInit
    fun beforeApiInit() = PluginHandler(null).addListener(this)
}
