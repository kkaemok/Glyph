plugins {
    alias(libs.plugins.conventions.bootstrap)
    alias(libs.plugins.conventions.velocity)
    alias(libs.plugins.resourcefactory.velocity)
    alias(libs.plugins.shadow)
}

velocityPluginJson {
    main = "$group.bootstrap.velocity.VelocityBootstrapImpl"
    version = rootProject.version.toString()
    id = "glyph"
    name = "Glyph"
    authors = listOf("Glyph contributors", "toxicity188 (BetterHud)")
    description = "Make a hud in minecraft!"
    url = "https://hangar.papermc.io/toxicity188/Glyph"
}

dependencies {
    shade(project(":glyph-velocity-api")) { isTransitive = false }
    shade(libs.bstats.velocity)
    shade(libs.kotlinStdlib)
}

val shade = configurations.getByName("shade")
val targetAttribute = manifestAttribute
val groupString = group.toString()

tasks {
    jar {
        finalizedBy(shadowJar)
    }
    shadowJar {
        configurations = listOf(shade)
        archiveBaseName = "${rootProject.name}-velocity"
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
        mergeServiceFiles()
    }
}

