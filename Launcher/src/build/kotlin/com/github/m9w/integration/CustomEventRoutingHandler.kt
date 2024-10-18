package com.github.m9w.integration

import com.github.m9w.customeventbroker.CustomEvent
import com.github.m9w.customeventbroker.CustomEventBroker
import com.github.m9w.customeventbroker.CustomEventHandler
import com.github.m9w.hooks.AfterApiInit
import com.github.m9w.hooks.EachBotTick
import com.github.manolo8.darkbot.Main
import eu.darkbot.api.API.Singleton
import eu.darkbot.api.events.Listener
import eu.darkbot.impl.PluginApiImpl
import io.netty.buffer.PooledByteBufAllocator
import java.io.IOException
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.SocketChannel
import java.nio.charset.StandardCharsets
import java.util.*
import kotlin.concurrent.Volatile

object CustomEventRoutingHandler : Listener, Singleton {
    private val PORT = System.getProperty("PORT", "-1").toInt()
    private val buffer = PooledByteBufAllocator.DEFAULT.buffer()
    private val sendQueue = LinkedList<ByteBuffer>()
    val unique = ProcessHandle.current().pid().toString().take(10).padStart(10, '0')

    @Volatile
    private var isActive = false
    private var lastAttemptInstall: Long = 0
    private var socketChannel: SocketChannel = SocketChannel.open()
    private var selector: Selector = Selector.open()

    init {
        CustomEventBroker.registerListener(this)
        if (PORT == -1) lastAttemptInstall = Long.MAX_VALUE
    }

    private fun install() {
        lastAttemptInstall = System.currentTimeMillis()
        try {
            selector = Selector.open()
            socketChannel = SocketChannel.open()
            socketChannel.configureBlocking(false)
            socketChannel.register(selector, SelectionKey.OP_READ or SelectionKey.OP_CONNECT or SelectionKey.OP_WRITE)
            socketChannel.connect(InetSocketAddress("localhost", PORT))
            isActive = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun uninstall() {
        try {
            isActive = false
            buffer.clear()
            selector.close()
            socketChannel.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @EachBotTick
    fun onTickTask() {
        if (!isActive) return
        try {
            selector.select {
                if (it.isValid && it.isConnectable) onConnect(it)
                if (it.isValid && it.isReadable) onRead(it)
                if (it.isValid && it.isWritable) onWrite(it)
                if (!it.isValid) uninstall()
            }
        } catch (e: IOException) {
            e.printStackTrace()
            uninstall()
        }
    }

    private fun onConnect(key: SelectionKey) {
        (key.channel() as SocketChannel).finishConnect()
    }

    private fun onWrite(key: SelectionKey) {
        val channel = key.channel() as SocketChannel
        synchronized(this) {
            while (sendQueue.isNotEmpty()) channel.write(sendQueue.removeFirst())
        }
    }

    private fun onRead(key: SelectionKey) {
        val channel = key.channel() as SocketChannel
        buffer.capacity(buffer.writerIndex()+1024)
        val numRead = channel.read(buffer.nioBuffer(buffer.writerIndex(), 1024))
        if (numRead == -1) throw IOException()
        buffer.writerIndex(buffer.writerIndex() + numRead)
        val offset = buffer.indexOf(buffer.writerIndex(), buffer.readerIndex(), 10) + 1
        if (offset == 0) return
        val data = ByteArray(offset).also(buffer::readBytes)
        val i = data.indexOf(58)
        try {
            Main.INSTANCE.addTask {
                CustomEvent(decode(String(data, 0, i)), decode(String(data, i + 1, data.size - i - 2)))
            }
        } finally {
            if (buffer.readableBytes() == 0) buffer.clear()
        }
    }

    @CustomEventHandler(regEx = "^!|^\\?\\d{10}!.*")
    fun sendEvent(event: CustomEvent) {
        if (isActive) {
            val data= ByteBuffer.wrap(encode(event.event.dropWhile { it == '!' }) + 58 + encode(event.value) + 10)
            synchronized(this) { sendQueue.addLast(data) }
        } else sendQueue.clear()
    }

    @CustomEventHandler(regEx = "^\\?\\d{10}!.*")
    fun handleDirect(message: String, event: String) {
        if (event.startsWith("?$unique!")) CustomEvent(event.substring(12), message)
    }

    @CustomEventHandler("?echo")
    fun echo() = CustomEvent("!echo", unique)

    fun onBackgroundTick() {
        if (!isActive && lastAttemptInstall + 1000 < System.currentTimeMillis()) {
            uninstall()
            install()
        }
    }

    fun isActive(): Boolean = isActive

    private fun encode(s: String): ByteArray = Base64.getEncoder().encode(s.toByteArray(StandardCharsets.UTF_8))

    private fun decode(s: String?): String = String(Base64.getDecoder().decode(s), StandardCharsets.UTF_8)
}
