plugins { id("standard-conventions") }
val shade = configurations.create("shade")
configurations.implementation { extendsFrom(shade) }
sourceSets.main { resources.srcDirs(rootDir.resolve("common-resources")) }
dependencies {
    shade(project(":glyph-core")) { isTransitive = false }
    shade(project(":glyph-api")) { isTransitive = false }
    shade(libs.betterCommand) { isTransitive = false }
    shade(libs.bundles.library)
    shade("com.mysql:mysql-connector-j:9.2.0")
    compileOnly(libs.bundles.adventure)
}
