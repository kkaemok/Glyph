package online.libang.glyph.shader

import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.*

class ShaderTemplateStoreTest {
    @TempDir lateinit var directory: Path
    private fun resource(name: String) = requireNotNull(javaClass.getResourceAsStream("/$name"))
        .use { it.readAllBytes().toString(Charsets.UTF_8) }
    private val bundled get() = resource("shader/26.3/text.vsh")
    private val previous get() = resource("fixtures/shader/26.3/text-v4.vsh").replace("\r\n", "\n")
    private fun read(path: Path) = ShaderTemplateStore.read(path, bundled, ShaderTemplateStore.previous263VertexHashes)

    @Test fun `new installations receive the specialized shader`() {
        val path = directory.resolve("26.3/text.vsh")
        assertEquals(bundled.lines(), read(path))
        assertEquals(bundled, Files.readString(path))
        assertContains(bundled, "#if GLYPH_SHADER_PROPERTIES != 0 || GLYPH_STATIC_SCALE")
    }

    @Test fun `existing stock template upgrades on both LF and CRLF installations`() {
        for (source in listOf(previous, previous.replace("\n", "\r\n"))) {
            val path = directory.resolve("text.vsh")
            Files.writeString(path, source)
            assertEquals(bundled.lines(), read(path))
            assertEquals(bundled, Files.readString(path))
        }
    }

    @Test fun `custom shader content is never replaced even if only a comment changed`() {
        val path = directory.resolve("text.vsh")
        val custom = previous + "\n// User shader extension\n"
        Files.writeString(path, custom)
        assertEquals(custom.lines(), read(path))
        assertEquals(custom, Files.readString(path))
    }

    @Test fun `current stock template remains untouched on repeated reloads`() {
        val path = directory.resolve("text.vsh")
        read(path)
        val modified = Files.getLastModifiedTime(path)
        assertEquals(bundled.lines(), read(path))
        assertEquals(modified, Files.getLastModifiedTime(path))
    }
}
