package com.github.m9w.main

import java.io.File
import java.io.FileOutputStream
import java.net.URLDecoder
import java.util.prefs.Preferences
import java.util.zip.ZipFile


fun main(args: Array<String>) {
    class M
    Preferences.userRoot().node("/eu/darkbot/verifier").putLong("DBOT_FIRST_RUN", Long.MAX_VALUE)
    val container = File(URLDecoder.decode(M::class.java.getProtectionDomain().codeSource.location.path, Charsets.UTF_8))
    val classes = String(M::class.java.getResource("/core.extract").openStream().readAllBytes()).replace("\\", "/").split("\n")

    ZipFile(container).use { zipEntry ->
        zipEntry.entries().asIterator().forEach { file ->
            if (classes.none { it.startsWith(file.name) }) return@forEach
            val name = "core/${file.name}"
            if (file.isDirectory) File(name).mkdirs()
            else FileOutputStream(name).use { zipEntry.getInputStream(file).transferTo(it) }
        }
    }
    
    var java = System.getProperty("java.home") + "/bin/javaw"
    val tag: String = System.getProperty("tag", "")
    if (tag.isNotEmpty()) {
        val tagged = File("$java$tag.exe")
        if (!tagged.isFile())  File("$java.exe").copyTo(tagged)
        java += tag
    }

    val argsProc: MutableList<String> = ArrayList()
    argsProc += java
    val dbg: String = System.getProperty("bg", "")
    if (dbg.isNotEmpty()) {
        val port = dbg.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]
        val wait = dbg.endsWith("y")
        argsProc.add("-agentlib:jdwp=transport=dt_socket,server=y,suspend=" + (if (wait) "y" else "n") + ",address=*:" + port)
    }
    argsProc += System.getProperties().filter { e -> !e.key.toString()[0].isLowerCase() }.map { "-D" + it.key + "=" + it.value }
    argsProc += listOf("-cp", "\"core/;${container.name}\"")
    argsProc += if (File("bot.ini").isFile()) "@bot.ini" else "com.github.manolo8.darkbot.Bot"
    argsProc += args
    if (dbg.isNotEmpty()) System.err.println(argsProc.joinToString(" "))
    println("<pid>" + Runtime.getRuntime().exec(argsProc.toTypedArray()).pid() + "</pid>")
}
