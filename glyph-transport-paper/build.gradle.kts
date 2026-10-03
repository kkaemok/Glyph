plugins { alias(libs.plugins.conventions.paperweight) }
dependencies {
    paperweight.paperDevBundle("26.1.2.build.+")
    compileOnly(project(":glyph-core"))
}
