plugins {
    kotlin("jvm") version "1.9.24"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.github.m9w.launcher"
version = "1.0-SNAPSHOT"

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    api("com.formdev:flatlaf:3.4")
    api("com.formdev:flatlaf-extras:3.4")
    api("com.google.code.gson:gson:2.11.0")
    api("com.miglayout:miglayout-swing:11.4.2")
    api("io.netty:netty-buffer:4.1.112.Final")
    api("org.jetbrains.kotlin:kotlin-reflect:2.0.20")
}

kotlin {
    jvmToolchain(17)
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "com.github.m9w.launcher.MainKt"
    }
}

tasks.shadowJar {
    archiveBaseName.set("DarkBotLauncher")
    archiveVersion.set("1.0.0")
    minimize()
    manifest {
        attributes["Main-Class"] = "com.github.m9w.launcher.MainKt"
    }
    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
}