plugins {
    alias(libs.plugins.conventions.standard)
}

dependencies {
    compileOnly(project(":glyph-paper-api"))
    compileOnly("org.geysermc.geyser:api:2.9.2-SNAPSHOT")
}