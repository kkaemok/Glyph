package online.libang.glyph.shader

import online.libang.glyph.location.PixelLocation
import kotlin.test.*

class RenderScaleTest {
    @Test fun `scale ordering is antisymmetric and agrees with equality`() {
        val scales = listOf(
            RenderScale.Scale(1.0, 1.0, false), RenderScale.Scale(1.0, 1.0, true),
            RenderScale.Scale(1.5, 1.0, false))
        val values = scales.flatMap { scale -> listOf(0, 10).map { RenderScale(PixelLocation(it, it, 1.0), scale) } }
        for (a in values) for (b in values) {
            assertEquals(a.compareTo(b).sign(), -b.compareTo(a).sign())
            assertEquals(a == b, a.compareTo(b) == 0)
            if (a == b) assertEquals(a.hashCode(), b.hashCode())
            for (c in values) if (a <= b && b <= c) assertTrue(a <= c)
        }
    }
    private fun Int.sign() = compareTo(0)
}
