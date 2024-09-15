package com.github.m9w.launcher.patches

import java.io.File

data class Patch(private val fileName: String) {
    val isOfficial = fileName.matches("^\\d*-.*".toRegex())
    var isActive: Boolean = isOfficial
    val name: String = fileName.split("-".toRegex(), 3).last().replace(".patch$".toRegex(), "")
    val prefix = fileName.split("-".toRegex(), 2).first()

    val files get() = listOf("DarkBot", "DarkBotAPI").map { it to File("Patches/$prefix-$it-$name.patch") }
            .filter { it.second.exists() }.toMap()
}
