package online.libang.glyph.image

import java.awt.image.BufferedImage
import kotlin.test.*

class ImageFeaturesTest {
    private fun source() = BufferedImage(5, 5, BufferedImage.TYPE_INT_ARGB).apply {
        for (y in 0 until 5) for (x in 0 until 5) setRGB(x, y, 0xff000000.toInt() or (y * 5 + x))
    }
    @Test fun `nine slice preserves all corners and tiles centre`() {
        val source = source()
        val image = NineSlice.resize(source, 11, 9, NineSlice.Insets(1, 1, 1, 1), NineSlice.Mode.TILE)
        assertEquals(source.getRGB(0, 0), image.getRGB(0, 0))
        assertEquals(source.getRGB(4, 0), image.getRGB(10, 0))
        assertEquals(source.getRGB(0, 4), image.getRGB(0, 8))
        assertEquals(source.getRGB(4, 4), image.getRGB(10, 8))
        assertEquals(source.getRGB(1, 1), image.getRGB(4, 4))
        assertEquals(source.getRGB(2, 0), image.getRGB(5, 0))
    }
    @Test fun `stretch samples centre and rejects impossible sizes`() {
        val source = source()
        val image = NineSlice.resize(source, 8, 8, NineSlice.Insets(1, 1, 1, 1))
        assertEquals(source.getRGB(3, 3), image.getRGB(6, 6))
        assertFailsWith<IllegalArgumentException> { NineSlice.resize(source, 1, 8, NineSlice.Insets(1, 1, 1, 1)) }
        assertFailsWith<IllegalArgumentException> { NineSlice.resize(source, 8, 8, NineSlice.Insets(3, 1, 2, 1)) }
        assertEquals(2, NineSlice.resize(source, 2, 2, NineSlice.Insets(1, 1, 1, 1)).width)
    }
    @Test fun `dynamic lookup stays bounded with hostile identifiers`() {
        val warnings = mutableListOf<String>()
        val lookup = DynamicAssetLookup(mapOf("skills/fire" to 1), 0, warnings::add)
        assertEquals(1, lookup.select("skills/fire"))
        repeat(1000) { assertEquals(0, lookup.select("missing/$it")) }
        lookup.select("missing/0")
        assertEquals(64, warnings.size)
    }
}
