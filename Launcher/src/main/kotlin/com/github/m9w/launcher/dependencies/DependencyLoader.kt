package com.github.m9w.launcher.dependencies

import com.github.m9w.launcher.properties.LauncherProperties
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.util.zip.ZipFile

object DependencyLoader {
    private val GRAAL_VM_URL = LauncherProperties.getProperty("GRAAL_VM_URL", "https://download.oracle.com/graalvm/17/latest/graalvm-jdk-17_windows-x64_bin.zip")

    private const val GRAAL_VM_PATH = "GraalVM"

    fun loadGraalVM() {
        val graalVMPath = LauncherProperties.getProperty(GRAAL_VM_PATH, "")
        if (graalVMPath.isNotEmpty() && File("$graalVMPath${File.separator}bin${File.separator}java.exe").isFile) {
            println("GraalVM: exist - $graalVMPath")
            return
        }

        val url = URL(GRAAL_VM_URL)
        val zip = File(url.file.split("/").last())
        println("GraalVM: downloading")
        zip.writeBytes(url.readBytes())
        println("GraalVM: unzipping")
        var currentFile = ""
        ZipFile(zip).apply {
            entries().asSequence().forEach {
                currentFile = "." + it.name
                File(currentFile).parentFile.mkdirs()
                getInputStream(it).apply { FileOutputStream(currentFile).also(this::transferTo).close() }
            }
        }
        if (currentFile.isNotEmpty()) {
            val graalVM = File(currentFile.split("/").first()).absolutePath
            LauncherProperties.setProperty(GRAAL_VM_PATH, graalVM)
            LauncherProperties.store()
            println(LauncherProperties.getProperty(GRAAL_VM_PATH))
        }
        zip.delete()
        println("GraalVM: done")
    }
}
