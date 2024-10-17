package com.github.m9w.builtin

import com.github.m9w.ReflectTool
import com.github.m9w.customeventbroker.CustomEventBroker
import com.github.m9w.customeventbroker.CustomEventHandler
import com.github.m9w.hooks.BeforeApiInit
import com.github.manolo8.darkbot.Main
import com.github.manolo8.darkbot.config.ConfigManager
import eu.darkbot.api.events.Listener
import java.io.File

object ConfigMangerEvents : Listener {
    private val configManager: ConfigManager get() = Main.INSTANCE.configManager

    @BeforeApiInit
    fun beforeApiInit() {
        CustomEventBroker.registerListener(this)
    }

    @CustomEventHandler("LAUNCHER!CHANGE_CONFIG")
    fun onLauncherChangeConfigEvent(config: String) {
        ReflectTool.of(configManager).setField("failedConfig", true)
        File(config).copyTo(configManager.configFile.toFile(), true)
        val cfg = Main.INSTANCE.configChange
        val current = configManager.configName
        ReflectTool.of(configManager).setField("configName", "")
        cfg.value = ""
        cfg.send(current)
    }

    @CustomEventHandler("LAUNCHER!STORE_CONFIG")
    fun onLauncherStoreConfigEvent(config: String) {
        File(config).writeText(ConfigManager.GSON.toJson(configManager.config))
    }
}
