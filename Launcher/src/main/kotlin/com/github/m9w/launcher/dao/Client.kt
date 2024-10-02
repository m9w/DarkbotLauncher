package com.github.m9w.launcher.dao

import com.github.m9w.launcher.launcher.ClientManager

data class Client (
    var name: String = "",
    var version: String = ClientManager.getLastBuildVersion(),
    var jvmID: String = "",
    var account: String = "",
    var config: String = "",
    val plugins: MutableSet<String> = HashSet(),
    val flags: MutableSet<ClientFlags> = HashSet()
)
