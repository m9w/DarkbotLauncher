package com.github.m9w.launcher.routers

import io.netty.buffer.PooledByteBufAllocator
import java.io.Closeable
import java.io.IOException
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.SelectionKey.*
import java.nio.channels.Selector
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.nio.charset.StandardCharsets
import java.util.*


/**
 * This class is creating a server socket on port from the first argument or using default 44567.
 * This router receives packages from clients and resends it to all other connected clients.
 */
class SimpleRouter(val port: Int = 44567) : Closeable {
    private val selector: Selector = Selector.open()
    private val server: ServerSocketChannel = ServerSocketChannel.open()

    init {
        server.bind(InetSocketAddress("localhost", port))
        server.configureBlocking(false)
        server.register(selector, OP_ACCEPT)
        Thread {
            while (true) {
                try {
                    selector.select {
                        if (it.isValid && it.isAcceptable) onAccept()
                        if (it.isValid && it.isConnectable) onConnect(it)
                        if (it.isValid && it.isReadable) onRead(it)
                        if (it.isValid && it.isWritable) onWrite(it)
                    }
                    Thread.sleep(10)
                } catch (e: Exception) {
                    Thread.sleep(100)
                }
            }
        }.apply { isDaemon = true }.start()
    }

    private fun onAccept() {
        server.accept().apply { finishConnect(); configureBlocking(false) }.register(selector, OP_CONNECT or OP_READ or OP_WRITE, Router())
    }

    private fun onConnect(key: SelectionKey) {
        val channel = key.channel() as SocketChannel
        if (channel.isConnectionPending) channel.finishConnect()
        channel.register(selector, OP_READ or OP_WRITE, key.attachment())
    }

    private fun onRead(key: SelectionKey) {
        val router = key.attachment() as Router
        try {
            router.read(key)
        } catch (e: IOException) {
            router.close()
            key.channel().close()
        }
    }

    private fun onWrite(key: SelectionKey) {
        (key.attachment() as Router).write(key)
    }

    private fun sendAll(bytes: ByteArray) {
        selector.keys().forEach { it.attachment().apply { if (this is Router) put(bytes) } }
    }

    fun sendAll(event: String, message: String) {
        sendAll(encode(event) + 58 + encode(message) + 10)
    }

    private inner class Router : Closeable {
        private val buffer = PooledByteBufAllocator.DEFAULT.buffer()
        private val sendQueue = LinkedList<ByteBuffer>()
        private var lock = false

        fun read(key: SelectionKey) {
            val channel = key.channel() as SocketChannel
            buffer.capacity(buffer.writerIndex()+1024)
            val numRead = channel.read(buffer.nioBuffer(buffer.writerIndex(), 1024))
            if (numRead == -1) throw IOException()
            buffer.writerIndex(buffer.writerIndex() + numRead)
            val offset = buffer.indexOf(buffer.writerIndex(), buffer.readerIndex(), 10) + 1
            if (offset == 0) return
            val data = ByteArray(offset)
            buffer.readBytes(data)
            lock = true
            sendAll(data)
            lock = false
            if (buffer.readableBytes() == 0) buffer.clear()
        }

        fun put(data: ByteArray) {
            if (sendQueue.size > 100) lock = true
            if (lock) return
            sendQueue.addLast(ByteBuffer.wrap(data))
        }

        fun write(key: SelectionKey) {
            val channel = key.channel() as SocketChannel
            while (sendQueue.isNotEmpty()) channel.write(sendQueue.removeFirst())
            lock = false
        }

        override fun close() {
            buffer.release()
        }
    }

    override fun close() {
        selector.close()
        server.close()
    }

    private fun encode(s: String): ByteArray = Base64.getEncoder().encode(s.toByteArray(StandardCharsets.UTF_8))

    private fun decode(s: String?): String = String(Base64.getDecoder().decode(s), StandardCharsets.UTF_8)
}
