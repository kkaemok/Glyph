import online.libang.glyph.api.version.MinecraftVersion
import online.libang.glyph.util.latestVersion
import kotlin.test.Test

class ModrinthTest {
    @Test
    fun testModrinth() {
        val latest = latestVersion(
            MinecraftVersion.LATEST,
            "paper"
        )
        println("Release: ${latest.release?.versionNumber}")
        println("Snapshot: ${latest.snapshot?.versionNumber}")
    }
}