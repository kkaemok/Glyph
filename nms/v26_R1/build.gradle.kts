plugins {
    alias(libs.plugins.conventions.paperweight)
}

dependencies {
    compileOnly(project(":glyph-transport-paper"))
    paperweight.paperDevBundle("26.1.2.build.+")
}
