pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()

        // Standard
        maven("https://repo.papermc.io/repository/maven-public/") //Paper
        maven("https://repo.codemc.org/repository/maven-public/")
        maven("https://repo.opencollab.dev/main/")
        maven("https://jitpack.io")

        // Bukkit
        maven("https://maven.enginehub.org/repo/") //WorldEdit, WorldGuard
        maven("https://nexus.phoenixdevt.fr/repository/maven-public/") //MMOItems, MMOCore, MythicLib
        maven("https://repo.skriptlang.org/releases") //Skript
        maven("https://repo.alessiodp.com/releases/") //Parties
        maven("https://repo.extendedclip.com/content/repositories/placeholderapi/") //PlaceholderAPI
        maven("https://mvn.lumine.io/repository/maven/") //MythicMobs
        maven("https://repo.momirealms.net/releases/") //CraftEngine
        maven("https://repo.nexomc.com/releases/") //Nexo

    }
}

rootProject.name = "Glyph"

include("glyph-api", "glyph-core", "glyph-paper-api", "glyph-velocity-api",
    "glyph-paper", "glyph-velocity", "glyph-transport-paper", "nms:v26_R1", "nms:v26_R2",
    "scheduler:paper", "bedrock:geyser", "bedrock:floodgate")
