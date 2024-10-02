package com.github.m9w.launcher.dao

enum class ClientFlags {
    Disable,
    Autostart,
    Hide,
    MinimizeToTray,
    NoAccount,
    Debug;

    companion object {
        fun toList() = entries.map { it.name }.toList()
    }
}
