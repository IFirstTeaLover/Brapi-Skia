pluginManagement {
    repositories {
        maven {
            name = "Fabric"
            url = uri("https://maven.fabricmc.net/")
        }
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.kikugie.dev/snapshots") { name = "KikuGie Snapshots" }
    }

    plugins {
        id("net.fabricmc.fabric-loom-remap") version providers.gradleProperty("loom_version")
        id("dev.kikugie.stonecutter") version "0.9.6"
    }
}

plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter {
    create(rootProject) {
        versions("1.21.11") // versions("1.20.1", "1.21.1", "1.21.11", "26.2")
        vcsVersion = "1.21.11"
    }
}

rootProject.name = "brapi"
