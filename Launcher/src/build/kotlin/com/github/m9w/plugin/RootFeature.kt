package com.github.m9w.plugin

import com.github.m9w.customeventbroker.CustomEvent
import com.github.m9w.customeventbroker.CustomEventHandler
import com.github.m9w.intergation.CustomEventRoutingHandler
import eu.darkbot.api.events.Listener
import eu.darkbot.api.extensions.Feature
import eu.darkbot.api.extensions.Task

@Feature(name = "Launcher integration", description = "", enabledByDefault = true)
class RootFeature : Task, Listener {
    private var shouldMinimize = System.getProperty("TRAY") == "true"

    override fun onTickTask() {
        if(shouldMinimize) {
            CustomEvent("LAUNCHER", "HIDE")
            shouldMinimize = false
            System.setProperty("TRAY", "false")
        }
        customEventHandler()
    }

    override fun onBackgroundTick() {
        customEventHandler()
        CustomEventRoutingHandler.onBackgroundTick()
    }

    private fun customEventHandler() {
        synchronized(CustomEventRoutingHandler) {
            CustomEventRoutingHandler.onTickTask()
        }
    }

    @CustomEventHandler("LAUNCHER")
    fun handler (value: String) {
        if(value == "STOP") Runtime.getRuntime().exit(0)
    }
}
