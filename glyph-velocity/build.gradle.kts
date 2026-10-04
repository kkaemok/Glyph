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
    description = "A high-performance HUD engine for Paper and Velocity, based on BetterHud."
    url = "https://github.com/toxicity188/BetterHud"
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
        filesMatching(listOf("META-INF/*.kotlin_module", "META-INF/services/**")) {
            duplicatesStrategy = DuplicatesStrategy.INCLUDE
        }
        configurations = listOf(shade)
        archiveBaseName = "${rootProject.name}-velocity"
        archiveClassifier = ""
        destinationDirectory = rootProject.layout.buildDirectory.dir("libs")
        manifest {
            attributes(targetAttribute)
        }
        dependencies {
            exclude(dependency("org.jetbrains:annotations:13.0"))
            // Gson appears in BetterHud's public method descriptors; use the platform's shared types.
            exclude(dependency("com.google.code.gson:gson:.*"))
        }
        fun prefix(pattern: String) {
            relocate(pattern, "$groupString.shaded.$pattern")
        }
        prefix("kotlin")
        // Keep command API identities consistent with the original BetterHud contracts.
        prefix("org.bstats")
        prefix("net.objecthunter.exp4j")
        prefix("net.jodah.expiringmap")
        prefix("com.zaxxer.hikari")
        prefix("org.yaml.snakeyaml")
        prefix("it.unimi.dsi.fastutil")
        mergeServiceFiles()
    }
}

