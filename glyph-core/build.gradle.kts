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
