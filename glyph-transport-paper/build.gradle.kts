plugins { alias(libs.plugins.conventions.paperweight) }
dependencies {
    paperweight.paperDevBundle("26.1.2.build.+")
    compileOnly(project(":glyph-core"))
    // Packet regressions run against the same mapped Paper classes used by the transport.
    testImplementation(files(configurations.compileClasspath))
}
