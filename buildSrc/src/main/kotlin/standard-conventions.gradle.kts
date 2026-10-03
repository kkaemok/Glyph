plugins {
    `java-library`
    kotlin("jvm")
    id("org.jetbrains.dokka")
}

group = "online.libang.glyph"
version = property("version").toString() + (BUILD_NUMBER?.let { "-SNAPSHOT-$it" } ?: "")

val targetJavaVersion = 25

dependencies {
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    api(libs.betterCommand)
}

tasks {
    test {
        useJUnitPlatform()
    }
    compileJava {
        options.encoding = Charsets.UTF_8.name()
    }
}

java {
    disableAutoTargetJvm()
    toolchain.languageVersion = JavaLanguageVersion.of(targetJavaVersion)
}

kotlin {
    jvmToolchain(targetJavaVersion)
}

dokka {
    moduleName = project.name
    dokkaSourceSets.configureEach {
        displayName = project.name
    }
}