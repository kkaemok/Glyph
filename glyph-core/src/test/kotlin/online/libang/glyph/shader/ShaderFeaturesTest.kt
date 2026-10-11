package online.libang.glyph.shader

import online.libang.glyph.location.GuiLocation
import online.libang.glyph.location.PixelLocation
import kotlin.test.*

class ShaderFeaturesTest {
    private fun shader(properties: Int = 0, static: Boolean = false) = HudShader(
        GuiLocation(50.0, 50.0), RenderScale(PixelLocation.zero, RenderScale.Scale(2.0, 1.0, static)),
        3, 0, 0.5, properties
    )

    @Test fun `ordinary HUD positioning opacity and relative scaling need no Globals`() {
        assertEquals(ShaderFeatures.NONE, ShaderFeatures.from(listOf(shader())))
        assertEquals(mapOf("GLYPH_SHADER_PROPERTIES" to "0", "GLYPH_STATIC_SCALE" to "0"),
            ShaderFeatures.from(emptyList()).constants())
    }

    @Test fun `all configured effect bits survive across distinct layouts`() {
        val features = ShaderFeatures.from(ShaderProperty.entries.map { shader(it.id) })
        assertEquals(7, features.properties)
        assertFalse(features.staticScale)
        for (property in ShaderProperty.entries)
            assertEquals(property.id, ShaderFeatures.from(listOf(shader(property.id))).properties)
    }

    @Test fun `static scaling requires screen size even without shader effects`() {
        assertEquals(ShaderFeatures(0, true), ShaderFeatures.from(listOf(shader(), shader(static = true))))
    }

    @Test fun `API extensions retain the full shared uniform and effect environment`() {
        val features = ShaderFeatures.NONE.withExtensions(true)
        assertEquals(7, features.properties)
        assertTrue(features.staticScale)
        assertEquals(ShaderFeatures.NONE, ShaderFeatures.NONE.withExtensions(false))
    }

    @Test fun `specialization is recalculated when effects and layouts are removed`() {
        assertEquals(ShaderFeatures(7, true), ShaderFeatures.from(listOf(shader(7, true))))
        assertEquals(ShaderFeatures.NONE, ShaderFeatures.from(emptyList()))
    }
}
