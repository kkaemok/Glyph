package online.libang.glyph.pack

import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.test.*

class PackTextureConflictTest {
    @TempDir lateinit var root: Path
    private val format = PackMeta.VersionFormat(97, 1)
    private fun write(name: String, files: Map<String, ByteArray>): Path = root.resolve(name).also {
        if (name.endsWith(".zip")) ZipPackOutput.write(CompiledPack.of(files), it)
        else DirectoryPackOutput().write(CompiledPack.of(files), it)
    }

    @Test fun `generated CNP and Glyph bars coexist despite different PNG bytes`() {
        val glyph = BossBarPackFixtures.glyphBars()
        val cnp = BossBarPackFixtures.cnpBars()
        assertFalse(glyph.contentEquals(cnp), "Must reproduce the original byte-conflict trigger")
        val primary = write("Glyph.zip", BossBarPackFixtures.glyphFiles(glyph))
        val other = write("CustomNameplates", mapOf(BossBarPackFixtures.BARS to cnp))
        val inspection = PackConflictInspector.inspect(primary, listOf(other), format)
        assertTrue(BossBarPackFixtures.BARS in inspection.equivalentTextures ||
            inspection.textureOwners.single().resource == BossBarPackFixtures.BARS)
        if (inspection.textureOwners.isNotEmpty()) assertEquals(other, inspection.textureOwners.single().owner)
    }

    @Test fun `transparent modern bossbar sprites compare pixels rather than invisible RGB`() {
        val black = BossBarPackFixtures.image(argb = 0)
        val white = BossBarPackFixtures.image(argb = 0xFFFFFF)
        assertFalse(black.contentEquals(white))
        val primary = write("glyph", mapOf(BossBarPackFixtures.SPRITE to black))
        val cnp = write("cnp.zip", mapOf(BossBarPackFixtures.SPRITE to white))
        val inspection = PackConflictInspector.inspect(primary, listOf(cnp), format)
        assertEquals(setOf(BossBarPackFixtures.SPRITE), inspection.equivalentTextures)
        assertTrue(inspection.textureOwners.isEmpty())
    }

    @Test fun `different color removal selects external atlas owner explicitly`() {
        val primary = write("glyph", BossBarPackFixtures.glyphFiles(BossBarPackFixtures.glyphBars(4)))
        val cnp = write("cnp.zip", mapOf(BossBarPackFixtures.BARS to BossBarPackFixtures.cnpBars(2)))
        val inspection = PackConflictInspector.inspect(primary, listOf(cnp), format)
        assertEquals(listOf(PackConflictInspector.TextureOwner(BossBarPackFixtures.BARS, cnp)), inspection.textureOwners)
    }

    @Test fun `ordinary texture dimensions and animation metadata have one external owner`() {
        val texture = "assets/shared/textures/gui/icon.png"
        val primary = write("glyph", mapOf(texture to BossBarPackFixtures.image(16, 16, -1),
            "$texture.mcmeta" to """{"animation":{"frametime":1}}""".toByteArray()))
        val other = write("external", mapOf(texture to BossBarPackFixtures.image(32, 32, -65536),
            "$texture.mcmeta" to """{"animation":{"frametime":2}}""".toByteArray()))
        assertEquals(listOf(PackConflictInspector.TextureOwner(texture, other)),
            PackConflictInspector.inspect(primary, listOf(other), format).textureOwners)
    }

    @Test fun `bitmap font dependencies protect PNGs outside the usual font texture directory`() {
        val texture = "assets/shared/textures/icons/coin.png"
        val font = "assets/shared/font/coin.json"
        val primary = write("glyph", mapOf(texture to BossBarPackFixtures.image(argb = -1),
            font to """{"providers":[{"type":"bitmap","file":"shared:icons/coin.png","height":8,"ascent":7,"chars":["x"]}]}""".toByteArray()))
        val external = write("external", mapOf(texture to BossBarPackFixtures.image(argb = -65536)))
        val error = assertFailsWith<IllegalArgumentException> { PackConflictInspector.validate(primary, listOf(external), format) }
        assertTrue(error.message!!.contains(texture))
    }

    @Test fun `font textures remain strict without a bundled provider JSON`() {
        val texture = "assets/minecraft/textures/font/ascii.png"
        val primary = write("glyph", mapOf(texture to BossBarPackFixtures.image(argb = -1)))
        val external = write("external", mapOf(texture to BossBarPackFixtures.image(argb = -65536)))
        assertFailsWith<IllegalArgumentException> { PackConflictInspector.validate(primary, listOf(external), format) }
    }

    @Test fun `font bitmap namespace IDs preserve a nested directory named textures`() {
        val texture = "assets/shared/textures/textures/icons/coin.png"
        val primary = write("glyph", mapOf(texture to BossBarPackFixtures.image(argb = -1),
            "assets/shared/font/coin.json" to
                """{"providers":[{"type":"minecraft:bitmap","file":"shared:textures/icons/coin.png","chars":["x"]}]}""".toByteArray()))
        val external = write("external", mapOf(texture to BossBarPackFixtures.image(argb = -65536)))
        assertFailsWith<IllegalArgumentException> { PackConflictInspector.validate(primary, listOf(external), format) }
    }

    @Test fun `post effect texture inputs stay strict even outside effect texture directories`() {
        val texture = "assets/shared/textures/gui/overlay.png"
        val primary = write("glyph", mapOf(texture to BossBarPackFixtures.image(argb = -1),
            "assets/minecraft/post_effect/end_of_frame.json" to
                """{"passes":[{"inputs":[{"sampler_name":"Overlay","location":"shared:textures/gui/overlay.png","width":182,"height":5}]}]}""".toByteArray()))
        val external = write("external", mapOf(texture to BossBarPackFixtures.image(argb = -65536)))
        val error = assertFailsWith<IllegalArgumentException> { PackConflictInspector.validate(primary, listOf(external), format) }
        assertTrue(error.message!!.contains(texture))
    }

    @Test fun `core shader font provider and post effect conflicts remain strict alongside safe bars`() {
        for (resource in listOf("assets/minecraft/shaders/core/text.vsh", "assets/shared/font/hud.json",
            "assets/minecraft/post_effect/end_of_frame.json")) {
            val primary = write("glyph.zip", mapOf(BossBarPackFixtures.BARS to BossBarPackFixtures.glyphBars(),
                resource to "{}".toByteArray()))
            val other = write("cnp", mapOf(BossBarPackFixtures.BARS to BossBarPackFixtures.cnpBars(),
                resource to "{\"different\":true}".toByteArray()))
            val error = assertFailsWith<IllegalArgumentException> { PackConflictInspector.validate(primary, listOf(other), format) }
            assertTrue(error.message!!.contains(resource))
        }
    }

    @Test fun `texture policy never silently chooses between different external owners`() {
        val texture = "assets/shared/textures/icon.png"
        val white = BossBarPackFixtures.image(argb = -1)
        val primary = write("glyph", mapOf(texture to white))
        val first = write("cnp", mapOf(texture to white))
        val second = write("other", mapOf(texture to BossBarPackFixtures.image(argb = -65536)))
        val error = assertFailsWith<IllegalArgumentException> { PackConflictInspector.validate(primary, listOf(first, second), format) }
        assertTrue(error.message!!.contains("Multiple external texture owners"))
    }

    @Test fun `same overlay directory with incompatible minor bounds fails even when inactive`() {
        fun metadata(minor: Int) = """{"pack":{"pack_format":97,"description":"test"},"overlays":{"entries":[{"directory":"shared","min_format":[97,$minor],"max_format":[97,$minor]}]}}""".toByteArray()
        val primary = write("glyph", mapOf("pack.mcmeta" to metadata(0)))
        val external = write("external", mapOf("pack.mcmeta" to metadata(1)))
        val error = assertFailsWith<IllegalArgumentException> { PackConflictInspector.validate(primary, listOf(external), format) }
        assertTrue(error.message!!.contains("Incompatible overlay 'shared'"))
    }

    @Test fun `overlay only texture cannot become global owner of a different primary PNG`() {
        val texture = "assets/shared/textures/icon.png"
        val primary = write("glyph", mapOf(texture to BossBarPackFixtures.image(argb = -1)))
        val external = write("external", mapOf("pack.mcmeta" to BossBarPackFixtures.metadata,
            "glyph_26_3/$texture" to BossBarPackFixtures.image(argb = -65536)))
        val error = assertFailsWith<IllegalArgumentException> { PackConflictInspector.validate(primary, listOf(external), format) }
        assertTrue(error.message!!.contains("external owner has no base asset"))
    }

    @Test fun `invalid and duplicate overlay declarations do not bypass conflict validation`() {
        for (entry in listOf(
            """{"directory":"../escape","formats":97}""",
            """{"directory":"same","min_format":[97,1],"max_format":[97,0]}""",
            """{"directory":"same","formats":97},{"directory":"same","formats":97}""")) {
            val primary = write("glyph", mapOf("pack.mcmeta" to
                """{"pack":{"pack_format":97,"description":"test"},"overlays":{"entries":[$entry]}}""".toByteArray()))
            assertFailsWith<IllegalArgumentException> { PackConflictInspector.validate(primary, emptyList(), format) }
        }
    }

    @Test fun `valid mixed case overlay directories follow Minecraft directory codec`() {
        val shader = "assets/minecraft/shaders/core/text.vsh"
        val primary = write("glyph", mapOf("pack.mcmeta" to
            """{"pack":{"pack_format":97,"description":"test"},"overlays":{"entries":[{"directory":"Valid_Overlay","formats":97}]}}""".toByteArray(),
            "Valid_Overlay/$shader" to "primary".toByteArray()))
        val external = write("external", mapOf(shader to "different".toByteArray()))
        PackConflictInspector.validate(primary, listOf(external), 84)
        val conflict = assertFailsWith<IllegalArgumentException> { PackConflictInspector.validate(primary, listOf(external), format) }
        assertTrue(conflict.message!!.contains(shader))
    }
}
