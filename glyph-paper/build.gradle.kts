import xyz.jpenilla.resourcefactory.bukkit.Permission

plugins {
    alias(libs.plugins.conventions.bootstrap)
    alias(libs.plugins.conventions.bukkit)
    alias(libs.plugins.resourcefactory.bukkit)
    alias(libs.plugins.shadow)
}

dependencies {
    shade(project(":glyph-transport-paper")) { isTransitive = false }
    shade(project(":glyph-paper-api")) { isTransitive = false }

    shade(project(":bedrock:geyser")) { isTransitive = false }
    shade(project(":bedrock:floodgate")) { isTransitive = false }

    shade(project(":scheduler:paper")) { isTransitive = false }

    shade(project(":nms:v26_R1")) { isTransitive = false }
    shade(project(":nms:v26_R2")) { isTransitive = false }

    shade(libs.bstats.bukkit)
    shade(libs.kotlinStdlib)

    compileOnly(shade(fileTree("shaded"))!!)

    compileOnly("io.lumine:Mythic-Dist:5.13.0")
    compileOnly("io.lumine:MythicLib-dist:1.7.1-SNAPSHOT")
    compileOnly("net.Indyuce:MMOCore-API:1.13.1-SNAPSHOT")
    compileOnly("net.Indyuce:MMOItems-API:6.10.1-SNAPSHOT")
    compileOnly("me.clip:placeholderapi:2.12.3")
    compileOnly("com.sk89q.worldedit:worldedit-bukkit:7.4.5") {
        exclude("com.google.guava")
        exclude("com.google.code.gson")
        exclude("it.unimi.dsi")
        exclude("org.apache.logging.log4j")
    }
    compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.0.19") {
        exclude("com.google.guava")
        exclude("com.google.code.gson")
        exclude("it.unimi.dsi")
        exclude("org.apache.logging.log4j")
    }
    compileOnly("com.github.MilkBowl:VaultAPI:1.7.1")
    compileOnly("com.github.SkriptLang:Skript:2.16.2")
    compileOnly("net.skinsrestorer:skinsrestorer-api:15.12.6")
    compileOnly("com.alessiodp.parties:parties-bukkit:3.2.18")
    compileOnly("net.momirealms:craft-engine-core:26.9.1")
    compileOnly("net.momirealms:craft-engine-bukkit:26.9.1")
    compileOnly("com.nexomc:nexo:1.28.0")
}

bukkitPluginYaml {
    main = "$group.bootstrap.bukkit.BukkitBootstrapImpl"
    version = project.version.toString()
    name = rootProject.name
    apiVersion = "26.1"
    authors = listOf("Glyph contributors", "toxicity188 (BetterHud)")
    description = "A high-performance HUD engine for Paper and Velocity, based on BetterHud."
    foliaSupported = true
    website = "https://www.spigotmc.org/resources/115559"
    softDepend = listOf(
        "MythicLib",
        "MythicMobs",
        "MMOCore",
        "MMOItems",
        "PlaceholderAPI",
        "WorldGuard",
        "Vault",
        "floodgate",
        "Geyser-Spigot",
        "Skript",
        "SkBee",
        "skript-placeholders",
        "skript-reflect",
        "SkinsRestorer",
        "Parties",
        "GPS",
        "BetterModel",
        "CraftEngine",
        "Nexo"
    )
    permissions {
        create("glyph.help") {
            description = "Accesses to help command."
            default = Permission.Default.OP
        }
        create("glyph.reload") {
            description = "Accesses to reload command."
            default = Permission.Default.OP
        }
        create("glyph.parse") {
            description = "Accesses to parse command."
            default = Permission.Default.OP
        }
        create("glyph.hud") {
            description = "Accesses to hud command."
            default = Permission.Default.OP
            children = mapOf(
                "glyph.hud.add" to true,
                "glyph.hud.remove" to true
            )
        }
        create("glyph.compass") {
            description = "Accesses to compass command."
            default = Permission.Default.OP
            children = mapOf(
                "glyph.compass.add" to true,
                "glyph.compass.remove" to true
            )
        }
        create("glyph.turn") {
            description = "Accesses to turn command."
            default = Permission.Default.OP
            children = mapOf(
                "glyph.turn.on" to true,
                "glyph.turn.off" to true,
                "glyph.turn.on.admin" to true,
                "glyph.turn.off.admin" to true
            )
        }
        create("glyph.pointer") {
            description = "Accesses to pointer command."
            default = Permission.Default.OP
            children = mapOf(
                "glyph.pointer.set" to true,
                "glyph.pointer.clear" to true,
                "glyph.pointer.remove" to true
            )
        }
        create("glyph.popup") {
            description = "Accesses to popup command."
            default = Permission.Default.OP
            children = mapOf(
                "glyph.popup.add" to true,
                "glyph.popup.remove" to true,
                "glyph.popup.show" to true,
                "glyph.popup.hide" to true
            )
        }
    }
}

val shade = configurations.getByName("shade")
val targetAttribute = manifestAttribute + mapOf("paperweight-mappings-namespace" to "mojang")
val groupString = group.toString()

tasks {
    jar {
        finalizedBy(shadowJar)
    }
    shadowJar {
        configurations = listOf(shade)
        archiveBaseName = "${rootProject.name}-paper"
        archiveClassifier = ""
        destinationDirectory = rootProject.layout.buildDirectory.dir("libs")
        manifest {
            attributes(targetAttribute)
        }
        dependencies {
            exclude(dependency("org.jetbrains:annotations:13.0"))
        }
        fun prefix(pattern: String) {
            relocate(pattern, "$groupString.shaded.$pattern")
        }
        prefix("kotlin")
        prefix("kr.toxicity.command.impl")
        prefix("org.bstats")
        prefix("net.objecthunter.exp4j")
        prefix("net.jodah.expiringmap")
        prefix("com.zaxxer.hikari")
        prefix("org.yaml.snakeyaml")
        prefix("com.google.gson")
        prefix("it.unimi.dsi.fastutil")
        relocate("kr.toxicity.hud.bootstrap.bukkit.compatibility.gps", "$groupString.bootstrap.bukkit.compatibility.gps")
        mergeServiceFiles()
        filesMatching(listOf("META-INF/*.kotlin_module", "META-INF/services/**")) {
            duplicatesStrategy = DuplicatesStrategy.INCLUDE
        }
    }
}

