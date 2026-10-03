plugins {
    alias(libs.plugins.conventions.paperweight)
}

dependencies {
    compileOnly(project(":glyph-transport-paper"))
    paperweight.paperDevBundle(providers.gradleProperty("paperModernVersion").getOrElse("26.3.build.+"))
}
