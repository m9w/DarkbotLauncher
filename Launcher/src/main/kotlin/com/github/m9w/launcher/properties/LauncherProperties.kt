package com.github.m9w.launcher.properties

import java.io.File
import java.util.Properties


object LauncherProperties : Properties() {
    private val propertiesFile get() = System.getProperty("props_file", "launcher.properties")
    private fun readResolve(): Any = LauncherProperties
    val debug get() = LauncherProperties.getProperty("debug", "").equals("true", true)

    init {
        load(File(propertiesFile).reader(Charsets.UTF_8))
    }

    fun store() {
        store(File(propertiesFile).writer(Charsets.UTF_8), "Darkbot launcher configuration")
    }
}