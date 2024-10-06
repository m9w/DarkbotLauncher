import java.io.FilenameFilter

plugins {
    kotlin("jvm") version "1.9.24"
    id("io.freefair.lombok") version "8.6"
}

group = "com.github.m9w.launcher"
version = "1.0-SNAPSHOT"

fun getDependencies(project: String): List<String> {
    val args = listOf("gradlew.bat", "--console", "plain", "dependencies", "--configuration", "api").toTypedArray()
    return String(Runtime.getRuntime().exec(args, null, File(projectDir, project)).inputStream.readAllBytes())
        .split("\r?\n".toRegex())
        .filter { it.startsWith("+---") || it.startsWith("\\---")}
        .map { it.substring(5).trim(',').replace("\\s\\(.\\)$".toRegex(),"")}
        .filter { !it.startsWith("project :") }
}

repositories {
    mavenLocal()
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
    maven { url = uri("https://oss.jfrog.org/artifactory/oss-snapshot-local/com/formdev/") }
}

dependencies {
    compileOnly("org.jetbrains:annotations:24.1.0")
    getDependencies("DarkBot").forEach { api(it) }
}

kotlin {
    jvmToolchain(17)
}

sourceSets {
    main {
        kotlin.srcDirs("Launcher/src/build/kotlin")
        java.srcDirs(
            "DarkBotAPI/api/src/main/java",
            "DarkBotAPI/impl/src/main/java",
            "DarkBotAPI/shared/src/main/java",
            "DarkBotAPI/util/src/main/java",
            "DarkBot/src/main/java"
        )
        resources {
            srcDirs(
                "DarkBot/src/main/resources",
                "Launcher/src/build/resources"
            )
        }
    }
}


val target = File(project.layout.buildDirectory.get().asFile,"resources/core.extract")
tasks.register("generateNestedClassList") {
    dependsOn("compileJava")
    val results = File(project.layout.buildDirectory.get().asFile,"classes/java/main")
    fun getNestedFiles(dir: File, filter: FilenameFilter): Collection<File> {
        return (dir.listFiles(filter)?.toList() ?: emptyList()) +
                (dir.listFiles { f, _ -> f.isDirectory }?.mapNotNull { getNestedFiles(it, filter) }?.flatten() ?: emptyList())
    }
    val sources = File(projectDir, "DarkBot/src/main/java")
    val darkbotSources = getNestedFiles(sources) { _, n -> n.endsWith(".java") }.map { it.toRelativeString(sources).dropLast(5) }
    doLast {
        target.parentFile.mkdirs()
        getNestedFiles(results) { _, n -> n.endsWith(".class") }
            .map {it.toRelativeString(results)}
            .filter { darkbotSources.contains(it.dropLast(6).replace("\\$.*$".toRegex(), "")) }
            .joinToString("\n")
            .let(target::writeText)
    }
}

tasks.processResources {
    dependsOn("generateNestedClassList")
    from(target.parentFile) { include(target.name) }
}

tasks.jar {
    archiveBaseName.set("CustomDarkBot")
    archiveVersion.set(System.getenv("CUSTOM_VERSION") ?: "0.0.1")
    manifest {
        attributes["SplashScreen-Image"] = "icon.png"
        attributes["Main-Class"] = "com.github.m9w.main.MainKt"
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(configurations.runtimeClasspath.get().map(::zipTree))
    destinationDirectory.set(File(projectDir, "release"))
    exclude("META-INF/**")
}