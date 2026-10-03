package online.libang.glyph.pack

import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipInputStream
import kotlin.test.*

class PackCompilerTest {
    @TempDir lateinit var directory: Path
    private fun pack() = CompiledPack.of(mapOf("assets/glyph/font/hud.json" to "font".toByteArray(), "pack.mcmeta" to "meta".toByteArray()))
    @Test fun `order and ZIP timestamps are deterministic`() {
        val original = pack()
        val reversed = CompiledPack.of(original.files.entries.reversed().associate { it.key to it.value })
        assertEquals(original.hash, reversed.hash)
        assertContentEquals(ZipPackOutput.bytes(original), ZipPackOutput.bytes(reversed))
    }
    @Test fun `paths participate in hash and byte boundaries are framed`() {
        assertNotEquals(CompiledPack.of(mapOf("a" to byteArrayOf(1))).hash,
            CompiledPack.of(mapOf("b" to byteArrayOf(1))).hash)
        assertNotEquals(CompiledPack.of(mapOf("a" to byteArrayOf(1), "b" to byteArrayOf(2))).hash,
            CompiledPack.of(mapOf("a" to byteArrayOf(1, 2), "b" to byteArrayOf())).hash)
    }
    @Test fun `conflicts report both owners including end of frame`() {
        val path = "assets/minecraft/post_effect/end_of_frame.json"
        val builder = CompiledPack.Builder().add(path, byteArrayOf(1), "CraftEngine")
        val error = assertFailsWith<PackConflictException> { builder.add(path, byteArrayOf(2), "Glyph") }
        assertTrue(error.message!!.contains("CraftEngine"))
        builder.add(path, byteArrayOf(1), "identical")
        assertEquals(1, builder.build().files.size)
    }
    @Test fun `invalid paths and namespaces are rejected`() {
        for (path in listOf("../x", "/x", "C:/x", "a\\b", "a//b", "assets/Bad/font/x.json", "assets/glyph/font/A.json"))
            assertFailsWith<IllegalArgumentException> { CompiledPack.validatePath(path) }
    }
    @Test fun `ZIP to folder switch and missing files recover`() {
        val pack = pack()
        val zip = directory.resolve("pack.zip")
        val folder = directory.resolve("pack")
        ZipPackOutput.write(pack, zip)
        DirectoryPackOutput().write(pack, folder)
        Files.delete(folder.resolve("pack.mcmeta"))
        DirectoryPackOutput().write(pack, folder)
        assertContentEquals(pack.files.getValue("pack.mcmeta"), Files.readAllBytes(folder.resolve("pack.mcmeta")))
        val contents = HashMap<String, ByteArray>()
        ZipInputStream(Files.newInputStream(zip)).use { input ->
            while (true) { val entry = input.nextEntry ?: break; contents[entry.name] = input.readAllBytes() }
        }
        pack.files.forEach { (name, bytes) -> assertContentEquals(bytes, contents[name]) }
    }
    @Test fun `changed and obsolete folder assets are repaired`() {
        val folder = directory.resolve("pack")
        DirectoryPackOutput().write(pack(), folder)
        Files.writeString(folder.resolve("pack.mcmeta"), "broken")
        Files.writeString(folder.resolve("obsolete"), "old")
        DirectoryPackOutput().write(pack(), folder)
        assertEquals("meta", Files.readString(folder.resolve("pack.mcmeta")))
        assertFalse(Files.exists(folder.resolve("obsolete")))
    }
}
