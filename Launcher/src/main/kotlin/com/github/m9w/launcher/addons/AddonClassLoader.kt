package com.github.m9w.launcher.addons

import java.net.URL
import java.net.URLClassLoader

class AddonClassLoader(urls: Array<out URL>) : URLClassLoader(urls) {
    fun defineClass(name: String, data: ByteArray) = this.defineClass(name, data, 0, data.size)
}