plugins {
    alias(libs.plugins.conventions.standard)
    id("xyz.jpenilla.run-paper") version "3.1.0"
}

val paper = project(":glyph-paper")
val velocity = project(":glyph-velocity")
tasks.register("pluginJar") { dependsOn(paper.tasks.named("build")) }
tasks.register("velocityJar") { dependsOn(velocity.tasks.named("build")) }
tasks.named("build") { dependsOn(paper.tasks.named("build"), velocity.tasks.named("build")) }
runPaper { disablePluginJarDetection() }
tasks.runServer {
    version(property("minecraft_version").toString())
    pluginJars(paper.tasks.named<Jar>("shadowJar").flatMap { it.archiveFile })
}
