package com.github.m9w.launcher.launcher

import com.github.m9w.launcher.dao.Client
import com.github.m9w.launcher.dao.ClientFlags
import com.github.m9w.launcher.getRouter
import com.github.m9w.launcher.properties.LauncherProperties
import com.google.gson.Gson
import java.io.File
import java.util.function.Consumer
import kotlin.collections.ArrayList
import kotlin.collections.LinkedHashMap
import kotlin.properties.Delegates
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KMutableProperty0
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1

object ClientManager {
    private var gson = Gson()
    var space = File(LauncherProperties.getProperty("space", "../DarkBot_space"))
    val clients = ArrayList<ClientImpl>()

    private val releaseDir = File("release")
    private val configsDir = File("configs")
    private val pluginsDir = File("plugins")
    private val accountsDir = File("accounts")

    init {
        LauncherProperties.apply {
            var i = 0
            for (cli in generateSequence { getProperty("client_${i++}") } )
                clients.add(ClientImpl(gson.fromJson(cli, Client::class.java)))
        }
        if(!releaseDir.isDirectory) releaseDir.mkdirs()
        if(!configsDir.isDirectory) configsDir.mkdirs()
        if(!pluginsDir.isDirectory) pluginsDir.mkdirs()
        if(!accountsDir.isDirectory) accountsDir.mkdirs()
    }

    fun newClient(): ClientImpl {
        val client = ClientImpl(Client())
        client.name = "new"
        clients.add(client)
        return client
    }

    class ClientImpl(val client: Client) {
        var name by linkedTo(client::name) { client.name = rename(it) }
        var version by linkedTo(client::version)
        var jvmID by linkedTo(client::jvmID)
        var account by linkedTo(client::account)
        var config by linkedTo(client::config)
        var plugins by linkedTo(client::plugins) { client.plugins.sync(it) }
        var flags by linkedTo(client::flags) { client.flags.sync(it) }

        private var pid = readPid()
        val unique get() = pid.toString().take(10).padStart(10, '0')

        private val updatesListeners = LinkedHashMap<KProperty1<Client, *>, Consumer<Any>>()

        fun <T : Any> addListener(parameter: KProperty1<Client, T>, listener: Consumer<T>) {
            updatesListeners[parameter] = listener as Consumer<Any>
        }

        private val dir get() = File(space, name)

        private fun rename(newName: String): String {
            var new = newName.trim()
            if (new.isEmpty()) new = "(empty)"
            val names = clients.filter { it !== this }.map { it.name }
            if (names.contains(new)) {
                var i = 2
                new = new.replace("\\s\\(\\d+\\)$".toRegex(), "")
                while (names.contains("$new ($i)")) i++
                new = "$new ($i)"
            }
            try {
                if (dir.isDirectory && name.isNotEmpty()) dir.renameTo(File(space, new))
            } catch (t: Throwable) {
                t.printStackTrace()
            }
            return new
        }

        fun clone(): ClientImpl {
            val newClient = ClientImpl(client.copy(name = ""))
            newClient.name = client.name
            clients.add(newClient)
            return newClient
        }

        fun start() {
            if (name.isEmpty()) return
            if (version.isEmpty()) return
            if (!dir.exists()) dir.mkdirs()

            val jvm = LauncherProperties.getProperty("GraalVM") + "/bin/java.exe"
            val jvmArgs = ArrayList<String>()
            val botArgs = ArrayList<String>()

            buildPreparations()
            configPreparations()
            pluginPreparation()

            if (flags.contains(ClientFlags.MinimizeToTray)) jvmArgs += "-DTRAY=true"
            if (flags.contains(ClientFlags.Debug)) jvmArgs += "-Dbg=5005" //suspend: "-Dbg=5005:y"
            if (flags.contains(ClientFlags.Autostart)) botArgs += "-start"
            if (flags.contains(ClientFlags.Hide)) botArgs += "-hide"
            if (flags.contains(ClientFlags.NoAccount)) botArgs += "-no_op"
            else if (accountPreparation()) botArgs += listOf("-login", "cred.ini")

            jvmArgs += "-DPORT=" + getRouter().port
            if (jvmID.isNotEmpty()) jvmArgs += "-Dtag=$jvmID"

            jvmArgs += listOf("-jar", "CustomDarkBot.jar")

            val args = listOf(jvm) + jvmArgs + botArgs
            if (LauncherProperties.debug) println("Run: " + args.joinToString(" "))
            if (flags.contains(ClientFlags.Disable)) return
            val process = Runtime.getRuntime().exec(args.toTypedArray(), null, dir)
            process.waitFor()
            val errors = process.errorStream.readAllBytes().let(::String)
            if (errors.isNotEmpty()) System.err.println(errors)
            pid = process.inputStream.readAllBytes().let(::String).substringAfter("<pid>").substringBefore("</pid>").toInt()
        }

        private fun buildPreparations() {
            val name = "CustomDarkBot-$version.jar"
            val build = File(releaseDir, name)
            if (!build.isFile) throw RuntimeException("Build $name corrupted")
            val target = File(dir, "CustomDarkBot.jar")
            if (!target.isFile || target.length() != build.length()) build.copyTo(target, true)
        }

        private fun configPreparations() {
            if (config.isEmpty()) return
            val name = "$config.json"
            val config = File(configsDir, name)
            if (!config.isFile) throw RuntimeException("Config $name corrupted")
            val target = File(dir, "config.json")
            config.copyTo(target, true)
        }

        private fun accountPreparation(): Boolean {
            if (account.isEmpty()) return false
            val name = "$account.ini"
            val account = File(accountsDir, name)
            if (!account.isFile) throw RuntimeException("Config $name corrupted")
            val target = File(dir, "cred.ini")
            if (!target.isFile || target.length() != account.length()) account.copyTo(target, true)
            return true
        }

        private fun pluginPreparation() {
            val localPlugins = File(dir, "plugins")
            this.plugins.map { File(pluginsDir, "$it.jar") }
                .filter(File::isFile)
                .forEach {
                    val target = File(localPlugins, it.name)
                    if (!target.isFile || target.length() != it.length())
                        it.copyTo(target, true)
                }
            localPlugins.listFiles { _, s -> s.endsWith(".jar") && !plugins.contains(s.substring(0, s.length - 4)) }?.forEach { it.delete() }
        }

        private fun readPid(): Int {
            val f = File(dir, "curr.pid")
            if (!f.isFile) return -1
            return try {
                String(f.readBytes()).takeWhile { it.isDigit() }.toInt()
            } catch (e: Exception) {
                return -1
            }
        }

        fun stop() {
            getRouter().sendAll("?$unique!LAUNCHER", "STOP")
        }

        fun show() {
            getRouter().sendAll("?$unique!LAUNCHER", "SHOW")
        }

        fun hide() {
            getRouter().sendAll("?$unique!LAUNCHER", "HIDE")
        }

        fun extractConfig(name: String) {
            getRouter().sendAll("?$unique!LAUNCHER", "STORE_CONFIG")
            File(dir, "config.json").apply {
                if (!isFile) throw RuntimeException("Client hasn't config")
                val target = File(configsDir, "$name.json")
                if (target.isFile) throw RuntimeException("Config with this name already exist")
                copyTo(target)
            }
        }

        fun remove() = clients.remove(this).apply { store() }

        private fun <T : Any> linkedTo(prop: KProperty<T>, store: Consumer<T>): ReadWriteProperty<Any, T> {
            return Delegates.observable(prop.call()) { property, oldValue, newValue ->
                if (oldValue.hashCode() == newValue.hashCode()) return@observable
                if (LauncherProperties.debug) println(prop.name + " changed from $oldValue to $newValue")
                store.accept(newValue)
                store()
                updatesListeners[property]?.accept(newValue)
            }
        }

        private fun <T : Any> linkedTo(prop: KMutableProperty0<T>): ReadWriteProperty<Any, T> {
            return linkedTo(prop, prop::set)
        }
    }

    private fun store() {
        LauncherProperties.apply {
            clients.forEachIndexed { i, client -> setProperty("client_$i", gson.toJson(client.client)) }
            generateSequence (clients.size) { if (remove("client_${it}") != null) it + 1 else null }.toList()
            store()
        }
    }

    fun getAccounts() = listOf("") + getFiles(accountsDir, "ini")

    fun getConfigs() = listOf("") + getFiles(configsDir, "json")

    fun getPlugins() = getFiles(pluginsDir, "jar")

    fun getBuildVersions() = getFiles(releaseDir, "jar").filter { it.startsWith("CustomDarkBot-") }.map { it.substring(14) }

    fun getLastBuildVersion(): String = getBuildVersions().lastOrNull() ?: ""

    fun <T> MutableCollection<T>.sync(from: Collection<T>) {
        this.removeIf { !from.contains(it) }
        addAll(from)
    }

    private fun getFiles(dir: File, ext: String) = dir.list { _, s -> s.endsWith(".$ext") }
            ?.map { it.substring(0, it.length - ext.length - 1) }
            ?.toList() ?: emptyList()
}
