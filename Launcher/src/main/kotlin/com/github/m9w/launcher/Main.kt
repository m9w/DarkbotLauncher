package com.github.m9w.launcher

import com.github.m9w.launcher.addons.Addons
import com.github.m9w.launcher.dependencies.DependencyLoader
import com.github.m9w.launcher.gui.GUI
import com.github.m9w.launcher.routers.SimpleRouter

private var router = SimpleRouter()

fun main() {
    DependencyLoader.loadGraalVM()
    Addons
    GUI
}

fun getRouter() = router