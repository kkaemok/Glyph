plugins {
    id("standard-conventions")
    id("io.papermc.paperweight.userdev")
}

dependencies {
    compileOnly(project(":glyph-api"))
    compileOnly(project(":glyph-paper-api"))
}