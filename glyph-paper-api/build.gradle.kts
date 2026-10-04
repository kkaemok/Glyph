plugins {
    alias(libs.plugins.conventions.api)
    alias(libs.plugins.conventions.bukkit)
}

dependencies {
    api(project(":glyph-api"))
}

val compatibilityClasspath = sourceSets.main.get().compileClasspath
val compatibilityClasspathFile = layout.buildDirectory.file("reports/compatibility-classpath.txt")
tasks.register("writeCompatibilityClasspath") {
    description = "Export platform dependencies for the original BetterHud binary compatibility check."
    val exportedClasspath = compatibilityClasspath
    val exportedFile = compatibilityClasspathFile
    inputs.files(exportedClasspath).withPropertyName("platformClasspath")
    outputs.file(exportedFile)
    doLast {
        exportedFile.get().asFile.apply {
            parentFile.mkdirs()
            writeText(exportedClasspath.asPath)
        }
    }
}
