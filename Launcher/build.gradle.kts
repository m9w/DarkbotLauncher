plugins {
    kotlin("jvm") version "1.9.23"
}

group = "com.github.m9w.launcher"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    val flatLafVersion = "3.4"
    api("com.formdev", "flatlaf", flatLafVersion)
    api("com.formdev", "flatlaf-extras", flatLafVersion)
    api("com.google.code.gson:gson:2.11.0")
    api("com.miglayout:miglayout-swing:11.4.2")
    api("io.netty:netty-buffer:4.1.112.Final")
}

kotlin {
    jvmToolchain(17)
}
