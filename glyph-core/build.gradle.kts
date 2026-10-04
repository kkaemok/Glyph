plugins {
    alias(libs.plugins.conventions.standard)
}

dependencies {
    implementation(project(":glyph-api"))
    compileOnly(libs.bundles.adventure)
    compileOnly(libs.bundles.library)

    testImplementation(libs.bundles.adventure)
    testImplementation(libs.bundles.library)
}

tasks.processTestResources {
    from(rootProject.file("common-resources")) {
        include("bars.png", "background.png")
        into("bossbar-fixtures")
    }
}

tasks.register<JavaExec>("profileCore") {
    group = "verification"
    description = "Profile state, dependency cache and send decisions without Minecraft."
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass = "online.libang.glyph.renderer.CoreProfile"
    javaLauncher = javaToolchains.launcherFor { languageVersion = JavaLanguageVersion.of(25) }
    jvmArgs("-XX:StartFlightRecording=filename=${layout.buildDirectory.get()}/core-profile.jfr,settings=profile,dumponexit=true")
}
