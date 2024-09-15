package com.github.m9w.launcher

import com.github.m9w.launcher.dependencies.DependencyLoader
import com.github.m9w.launcher.gui.GUI


fun main() {
    DependencyLoader.loadGraalVM()
    GUI
}
