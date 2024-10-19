package com.github.m9w.plugin

import com.github.m9w.ReflectTool
import com.github.m9w.customeventbroker.CustomEvent
import com.github.m9w.customeventbroker.CustomEventHandler
import com.github.m9w.integration.CustomEventRoutingHandler
import com.github.m9w.plugin.monitor.ChangesHandler
import com.github.manolo8.darkbot.Main
import com.github.manolo8.darkbot.config.Config
import com.github.manolo8.darkbot.extensions.plugins.PluginHandler.PLUGIN_UPDATE_FOLDER
import com.github.manolo8.darkbot.gui.titlebar.MainTitleBar
import com.github.manolo8.darkbot.gui.titlebar.TrayButton
import eu.darkbot.api.events.Listener
import eu.darkbot.api.extensions.Feature
import eu.darkbot.api.extensions.Task
import java.awt.SystemTray
import java.awt.TrayIcon
import java.io.File

@Feature(name = "Launcher integration", description = "", enabledByDefault = true)
class RootFeature : Task, Listener {
    private var shouldMinimize = System.getProperty("TRAY") == "true"
    private val gui get() = Main.INSTANCE.gui
    private val trayButton: TrayButton get() = (gui.jMenuBar as MainTitleBar).components.find { it is TrayButton } as TrayButton
    private val icon: TrayIcon get() = ReflectTool.of(trayButton).getField("icon")
    private val handler = ChangesHandler()

    init {
        handler.addListener {
            if (!PLUGIN_UPDATE_FOLDER.isDirectory) PLUGIN_UPDATE_FOLDER.mkdirs()
            it.copyTo(PLUGIN_UPDATE_FOLDER, true)
            Main.INSTANCE.pluginHandler.updatePlugins()
        }
    }

    override fun onTickTask() {
        if(shouldMinimize) {
            CustomEvent("LAUNCHER", "HIDE")
            shouldMinimize = false
            System.setProperty("TRAY", "false")
        }
    }

    override fun onBackgroundTick() {
        CustomEventRoutingHandler.backgroundTick()
        handler.handleFile(try { ReflectTool.of(Main.INSTANCE.config.MISCELLANEOUS).getField<File>("PLUGIN_HANDLE") } catch (e: Exception) { null })
        handler.tick()
    }

    @CustomEventHandler("LAUNCHER")
    fun handler(action: String) {
        when (action) {
            "STOP" -> Runtime.getRuntime().exit(0)
            "HIDE" -> if(gui.isVisible) trayButton.actionPerformed(null)
            "SHOW" -> { SystemTray.getSystemTray().remove(icon); gui.isVisible = true }
        }
    }
}
