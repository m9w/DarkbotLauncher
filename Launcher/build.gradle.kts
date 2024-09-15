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
}

kotlin {
    jvmToolchain(17)
}
