package online.libang.glyph.pack

import java.nio.file.Files
import kotlin.test.*

class PackConflictInspectorTest {
    @Test fun `modern minor bounds select overlays without legacy formats`() {
        val root = Files.createTempDirectory("glyph-pack-minor")
        try {
            val primary = root.resolve("primary")
            val other = root.resolve("other")
            val shader = "assets/minecraft/shaders/core/text.vsh"
            DirectoryPackOutput(true).write(CompiledPack.of(mapOf(
                "pack.mcmeta" to """{"pack":{"description":{"text":"CraftEngine"},"min_format":[97,1],"max_format":[97,1]},"overlays":{"entries":[{"directory":"minor_one","min_format":[97,1],"max_format":[97,1]}]}}""".toByteArray(),
                "minor_one/$shader" to "glyph".toByteArray())), primary)
            DirectoryPackOutput(true).write(CompiledPack.of(mapOf(shader to "external".toByteArray())), other)
            PackConflictInspector.validate(primary, listOf(other), PackMeta.VersionFormat(97, 0))
            assertFailsWith<IllegalArgumentException> {
                PackConflictInspector.validate(primary, listOf(other), PackMeta.VersionFormat(97, 1))
            }
            PackConflictInspector.validate(primary, listOf(other), PackMeta.VersionFormat(97, 2))
        } finally { root.toFile().deleteRecursively() }
    }
    @Test fun `active overlay conflicts with external base shader`() {
        val root = Files.createTempDirectory("glyph-pack-overlay")
        try {
            val primary = root.resolve("primary")
            val other = root.resolve("other")
            DirectoryPackOutput(true).write(CompiledPack.of(mapOf(
                "pack.mcmeta" to """{"pack":{"pack_format":97,"description":"test"},"overlays":{"entries":[{"directory":"glyph_26_3","formats":{"min_inclusive":97,"max_inclusive":97}}]}}""".toByteArray(),
                "glyph_26_3/assets/minecraft/shaders/core/text.vsh" to "glyph".toByteArray())), primary)
            DirectoryPackOutput(true).write(CompiledPack.of(mapOf(
                "assets/minecraft/shaders/core/text.vsh" to "external".toByteArray())), other)
            PackConflictInspector.validate(primary, listOf(other), 84)
            val conflict = assertFailsWith<IllegalArgumentException> { PackConflictInspector.validate(primary, listOf(other), 97) }
            assertTrue(conflict.message!!.contains("assets/minecraft/shaders/core/text.vsh"))
            assertTrue(conflict.message!!.contains("primary"))
            assertTrue(conflict.message!!.contains("other"))
        } finally { root.toFile().deleteRecursively() }
    }
    @Test fun `stack conflict detects post effects in zip and folder equally`() {
        val root = Files.createTempDirectory("glyph-post-conflict")
        try {
            val primary = root.resolve("glyph.zip")
            val other = root.resolve("other")
            val name = "assets/minecraft/post_effect/end_of_frame.json"
            ZipPackOutput.write(CompiledPack.of(mapOf(name to "{}".toByteArray())), primary)
            DirectoryPackOutput(true).write(CompiledPack.of(mapOf(name to "{}".toByteArray())), other)
            PackConflictInspector.validate(primary, listOf(other), 97)
            Files.writeString(other.resolve(name), "{\"different\":true}")
            assertFailsWith<IllegalArgumentException> { PackConflictInspector.validate(primary, listOf(other), 97) }
        } finally { root.toFile().deleteRecursively() }
    }
}
