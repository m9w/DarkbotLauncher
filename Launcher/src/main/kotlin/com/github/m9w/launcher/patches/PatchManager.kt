package com.github.m9w.launcher.patches

import com.github.m9w.launcher.builder.BuildManager
import com.github.m9w.launcher.properties.LauncherProperties
import java.io.File


object PatchManager {
    private val patchFiles get() = (File("Patches").listFiles { _, n -> n.endsWith(".patch") } ?: emptyArray()).asSequence().toList()
    private val userName get() = LauncherProperties.getProperty("author", "user").replace("-|\\s".toRegex(), "_")

    fun storeChangesAsPatch(name: String) {
        val clearName = name.replace("\\s".toRegex(), "_")
        BuildManager.run("git submodule foreach \"git diff -p -B -M -C HEAD > ../Patches/$userName-\$name-$clearName.patch\"")
        BuildManager.run("git submodule foreach \"git reset --hard HEAD\"")
        patchFiles.filter { it.isFile && it.length() == 0L }.forEach(File::delete)
    }

    fun getPatches(all: Boolean = false) = patchFiles.map { Patch(it.name) }.filter { it.isOfficial || all }
        .distinctBy { it.name }.sortedBy { it.prefix }.toList()

    fun applyPatch(patch: Patch, revert: Boolean = false) {
        if (!patch.isActive) return
        val revertFlag = if (revert) " -R " else ""
        patch.files.forEach { (prj, patchFile) ->
            val patchPath = "Patches/${patchFile.name}"
            BuildManager.run("git apply $revertFlag --directory=$prj $patchPath")
        }
    }
}