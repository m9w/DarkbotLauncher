package com.github.m9w.plugin.monitor

import java.io.Closeable
import java.io.File
import java.nio.file.*
import java.nio.file.StandardWatchEventKinds.ENTRY_CREATE
import java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY
import java.util.concurrent.CopyOnWriteArraySet
import java.util.function.Consumer

class ChangesHandler :  Closeable {
    private val listeners = CopyOnWriteArraySet<Consumer<File>>()
    private var watchService: WatchService? = null
    private var file: File? = null

    fun addListener(listener: (File) -> Unit) = listeners.add(listener)

    fun handleFile(file: File?) {
        if (this.file == file) return
        close()
        this.file = file
        if (file == null) return
        watchService = FileSystems.getDefault().newWatchService().also { file.toPath().parent?.register(it, ENTRY_MODIFY, ENTRY_CREATE) }
    }

    fun tick() {
        var key: WatchKey? = watchService?.poll()
        while (key != null) {
            try {
                for (event in key.pollEvents()) {
                    val kind = event.kind()
                    if (kind != StandardWatchEventKinds.OVERFLOW) {
                        val eventPath = event.context() as Path
                        val directoryPath = key.watchable() as Path
                        if (directoryPath.resolve(eventPath).toFile() == file)
                            listeners.forEach { file?.let(it::accept) }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            key.reset()
            key = watchService?.poll()
        }
    }

    override fun close() {
        watchService?.close() // Close the WatchService
    }
}

