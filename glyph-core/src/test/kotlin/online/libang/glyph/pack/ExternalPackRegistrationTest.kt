package online.libang.glyph.pack

import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipFile
import kotlin.test.*

class ExternalPackRegistrationTest {
    @TempDir lateinit var root: Path
    private val format = PackMeta.VersionFormat(97, 1)
    private fun read(pack: Path): Map<String, ByteArray> = if (Files.isDirectory(pack)) {
        Files.walk(pack).use { paths -> paths.filter { Files.isRegularFile(it) }.toList().associate {
            pack.relativize(it).toString().replace('\\', '/') to Files.readAllBytes(it)
        } }
    } else ZipFile(pack.toFile()).use { zip -> zip.entries().toList().filterNot { it.isDirectory }.associate {
        it.name to zip.getInputStream(it).use { stream -> stream.readAllBytes() }
    } }

    @Test fun `CraftEngine cache receives Glyph shaders and fonts with CNP bars from zip or folder`() {
        for (folder in listOf(false, true)) {
            val glyph = root.resolve(if (folder) "Glyph" else "Glyph.zip")
            val cnp = root.resolve(if (folder) "CNP.zip" else "CNP")
            val ownFiles = BossBarPackFixtures.glyphFiles()
            val cnpFiles = mapOf(BossBarPackFixtures.BARS to BossBarPackFixtures.cnpBars(),
                "assets/customnameplates/font/default.json" to "{\"providers\":[]}".toByteArray())
            if (folder) { DirectoryPackOutput().write(CompiledPack.of(ownFiles), glyph); ZipPackOutput.write(CompiledPack.of(cnpFiles), cnp) }
            else { ZipPackOutput.write(CompiledPack.of(ownFiles), glyph); DirectoryPackOutput().write(CompiledPack.of(cnpFiles), cnp) }
            val folders = mutableSetOf<Path>(); val zips = mutableSetOf<Path>()
            (if (Files.isDirectory(cnp)) folders else zips).add(cnp)
            val registration = ExternalPackRegistration(root.resolve("cache/$folder.zip"))
            val result = registration.register(glyph, folders + zips, folders, zips, format)
            val registered = assertNotNull(registration.registeredOutput)
            assertTrue(registered in folders + zips)
            assertTrue(cnp in folders + zips)
            val delivered = read(registered)
            assertContentEquals(ownFiles.getValue(BossBarPackFixtures.SHADER), delivered[BossBarPackFixtures.SHADER])
            assertContentEquals(ownFiles.getValue(BossBarPackFixtures.FONT), delivered[BossBarPackFixtures.FONT])
            assertContentEquals(ownFiles.getValue("pack.mcmeta"), delivered["pack.mcmeta"])
            if (result.textureOwners.isNotEmpty()) assertFalse(BossBarPackFixtures.BARS in delivered)
            else assertTrue(BossBarPackFixtures.BARS in delivered)
            ownFiles.forEach { (name, bytes) -> assertContentEquals(bytes, read(glyph)[name]) }
            cnpFiles.forEach { (name, bytes) -> assertContentEquals(bytes, read(cnp)[name]) }
        }
    }

    @Test fun `different CNP removal color keeps one atlas owner and Glyph still registered`() {
        val glyph = root.resolve("Glyph.zip"); val cnp = root.resolve("CNP.zip")
        val ownFiles = BossBarPackFixtures.glyphFiles(BossBarPackFixtures.glyphBars(4))
        ZipPackOutput.write(CompiledPack.of(ownFiles), glyph)
        ZipPackOutput.write(CompiledPack.of(mapOf(BossBarPackFixtures.BARS to BossBarPackFixtures.cnpBars(2))), cnp)
        val folders = mutableSetOf<Path>(); val zips = mutableSetOf(cnp)
        val registration = ExternalPackRegistration(root.resolve("cache/resolved.zip"))
        registration.register(glyph, zips, folders, zips, format)
        val delivered = read(assertNotNull(registration.registeredOutput))
        assertFalse(BossBarPackFixtures.BARS in delivered)
        assertTrue(BossBarPackFixtures.FONT in delivered)
        assertTrue(BossBarPackFixtures.SHADER in delivered)
        assertTrue(cnp in zips)
        assertEquals(2, zips.size)
        assertContentEquals(ownFiles.getValue(BossBarPackFixtures.BARS), read(glyph)[BossBarPackFixtures.BARS])
    }

    @Test fun `registration removes base and active overlay texture duplicates and their metadata`() {
        val texture = "assets/shared/textures/icon.png"
        val glyph = root.resolve("Glyph")
        val files = BossBarPackFixtures.glyphFiles() + mapOf(texture to BossBarPackFixtures.image(argb = -1),
            "glyph_26_3/$texture" to BossBarPackFixtures.image(argb = -65536),
            "$texture.mcmeta" to "{}".toByteArray())
        DirectoryPackOutput().write(CompiledPack.of(files), glyph)
        val external = root.resolve("external")
        DirectoryPackOutput().write(CompiledPack.of(mapOf(texture to BossBarPackFixtures.image(argb = -16711936))), external)
        val folders = mutableSetOf(external); val zips = mutableSetOf<Path>()
        val registration = ExternalPackRegistration(root.resolve("resolved.zip"))
        registration.register(glyph, folders, folders, zips, format)
        val delivered = read(assertNotNull(registration.registeredOutput))
        assertFalse(texture in delivered)
        assertFalse("glyph_26_3/$texture" in delivered)
        assertFalse("$texture.mcmeta" in delivered)
        assertTrue(BossBarPackFixtures.SHADER in delivered)
    }

    @Test fun `regeneration excludes old registration then switches from resolved copy to original output`() {
        val glyph = root.resolve("Glyph.zip"); val external = root.resolve("CNP.zip")
        val externalTexture = BossBarPackFixtures.cnpBars(2)
        ZipPackOutput.write(CompiledPack.of(BossBarPackFixtures.glyphFiles()), glyph)
        ZipPackOutput.write(CompiledPack.of(mapOf(BossBarPackFixtures.BARS to externalTexture)), external)
        val folders = mutableSetOf<Path>(); val zips = mutableSetOf(external)
        val resolved = root.resolve("cache/resolved.zip")
        val registration = ExternalPackRegistration(resolved)
        registration.register(glyph, zips, folders, zips, format)
        assertEquals(resolved, registration.registeredOutput)
        val updated = BossBarPackFixtures.glyphFiles(externalTexture) +
            (BossBarPackFixtures.SHADER to "new shader".toByteArray())
        ZipPackOutput.write(CompiledPack.of(updated), glyph)
        registration.register(glyph, zips, folders, zips, format)
        assertEquals(glyph, registration.registeredOutput)
        assertEquals(setOf(external, glyph), zips)
        assertContentEquals("new shader".toByteArray(), read(glyph)[BossBarPackFixtures.SHADER])
    }

    @Test fun `critical conflict unregisters prior Glyph output but retains external cache entries`() {
        val glyph = root.resolve("Glyph.zip"); val external = root.resolve("CNP.zip")
        val effect = "assets/minecraft/post_effect/end_of_frame.json"
        ZipPackOutput.write(CompiledPack.of(BossBarPackFixtures.glyphFiles() + (effect to "{}".toByteArray())), glyph)
        ZipPackOutput.write(CompiledPack.of(mapOf(BossBarPackFixtures.BARS to BossBarPackFixtures.cnpBars(2))), external)
        val folders = mutableSetOf<Path>(); val zips = mutableSetOf(external)
        val registration = ExternalPackRegistration(root.resolve("cache/resolved.zip"))
        registration.register(glyph, zips, folders, zips, format)
        assertNotNull(registration.registeredOutput)
        ZipPackOutput.write(CompiledPack.of(mapOf(BossBarPackFixtures.BARS to BossBarPackFixtures.cnpBars(2),
            effect to "{\"different\":true}".toByteArray())), external)
        assertFailsWith<IllegalArgumentException> { registration.register(glyph, zips, folders, zips, format) }
        assertNull(registration.registeredOutput)
        assertEquals(setOf(external), zips)
        assertTrue(Files.exists(glyph))
        assertTrue(Files.exists(external))
    }
}
