plugins {
    id("standard-conventions")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:${property("minecraft_version")}.build.+")
    compileOnly("com.mojang:brigadier:1.3.10")
    compileOnly(libs.bundles.adventure)
}
