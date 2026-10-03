plugins {
    alias(libs.plugins.conventions.standard)
}

dependencies {
    compileOnly(project(":glyph-api"))
    compileOnly("io.papermc.paper:paper-api:${property("minecraft_version")}.build.+")
}