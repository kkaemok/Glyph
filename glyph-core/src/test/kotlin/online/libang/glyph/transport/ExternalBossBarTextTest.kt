package online.libang.glyph.transport

import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import kotlin.test.*

class ExternalBossBarTextTest {
    private val glyphFont = Key.key("glyph", "default")
    private val foreignFont = Key.key("another_plugin", "images")
    private fun measure(c: Component) = ExternalBossBarText.measure(c, glyphFont) { 5 }

    @Test fun `ordinary text keeps its original tree and implicit vanilla font`() {
        val original = Component.text("A B", NamedTextColor.RED)
        val measured = assertNotNull(measure(original))
        assertEquals(16, measured.width)
        val wrapper = measured.component.build()
        assertEquals(ExternalBossBarText.DEFAULT_FONT, wrapper.font())
        assertSame(original, wrapper.children().single())
        assertNull(original.font())
        // The enclosing renderer uses a space font. The wrapper must stop its inheritance.
        val rendered = Component.text().font(Key.key("glyph", "space")).append(wrapper).build()
        assertEquals(ExternalBossBarText.DEFAULT_FONT, rendered.children().single().font())
    }

    @Test fun `nested styles and explicit known fonts are preserved without recursion rewriting`() {
        val original = Component.text("A").font(glyphFont).decorate(TextDecoration.BOLD)
            .append(Component.text("B").font(ExternalBossBarText.DEFAULT_FONT).decoration(TextDecoration.BOLD, false)
                .append(Component.text("C").decorate(TextDecoration.ITALIC)))
        val before = GsonComponentSerializer.gson().serialize(original)
        val measured = assertNotNull(measure(original))
        assertEquals(19, measured.width) // 7 + 6 + 6; italic changes shape, not advance.
        assertSame(original, measured.component.build().children().single())
        assertEquals(before, GsonComponentSerializer.gson().serialize(original))
        assertEquals(glyphFont, original.font())
        assertEquals(ExternalBossBarText.DEFAULT_FONT, original.children().single().font())
    }

    @Test fun `unknown root font is never overwritten or given a fabricated width`() {
        val original = Component.text("text").font(foreignFont)
        assertFalse(ExternalBossBarText.canMerge(original, glyphFont))
        assertNull(ExternalBossBarText.measure(original, glyphFont) { error("Must not measure foreign fonts") })
        assertEquals(foreignFont, original.font())
    }

    @Test fun `unknown nested font excludes the entire component from shared centering`() {
        val child = Component.text("icon").font(foreignFont).decorate(TextDecoration.OBFUSCATED)
        val original = Component.text("label").append(Component.empty().append(child))
        assertNull(measure(original))
        assertSame(child, original.children().single().children().single())
        assertEquals(foreignFont, child.font())
    }

    @Test fun `private use glyphs are unsafe even when default font is implicit`() {
        for (point in listOf(0xE001, 0xF0001, 0x100001)) {
            val original = Component.text(String(Character.toChars(point)))
            assertNull(ExternalBossBarText.measure(original, glyphFont) { error("No advance in the packet") })
        }
    }

    @Test fun `positive and negative offsets in arbitrary space fonts stay client measured`() {
        val offsets = Key.key("arbitrary", "spacing")
        // A pack can assign -64 and +96, or different advances, to precisely these same characters.
        val original = Component.text("\uF800\uF830").font(offsets)
        assertNull(ExternalBossBarText.measure(original, glyphFont) { error("Cannot infer signed advances") })
        assertEquals(offsets, original.font())
        assertEquals("\uF800\uF830", original.content())
    }

    @Test fun `CNP like background shift image and translated children survive as one tree`() {
        val original = cnpTitle()
        val before = GsonComponentSerializer.gson().serialize(original)
        assertNull(measure(original))
        assertEquals(before, GsonComponentSerializer.gson().serialize(original))
        assertEquals(Key.key("customnameplates", "default"), original.font())
        assertEquals(Key.key("customnameplates", "shift_1"), original.children()[1].font())
        assertEquals(Key.key("customnameplates", "shift_2"), original.children()[2].font())
    }

    @Test fun `translatable arguments fallback and decorations remain entirely client resolved`() {
        val original = Component.translatable("example.title", "Hello %s",
            Component.text("argument").font(foreignFont)).decorate(TextDecoration.UNDERLINED)
        assertNull(measure(original))
        assertEquals(foreignFont, (original.arguments().single().value() as Component).font())
        assertEquals("Hello %s", original.fallback())
    }

    @Test fun `client resolved and multiline components remain standalone`() {
        assertNull(measure(Component.keybind("key.jump")))
        assertNull(measure(Component.selector("@p")))
        assertNull(measure(Component.score("Player", "objective")))
        assertNull(measure(Component.text("first\nsecond")))
    }

    companion object {
        fun cnpTitle(): Component = Component.empty().font(Key.key("customnameplates", "default"))
            .append(Component.text("\uE001\uF807")) // background bitmap, then negative advance
            .append(Component.text("12:00").font(Key.key("customnameplates", "shift_1")))
            .append(Component.text("\uE002").font(Key.key("customnameplates", "shift_2"))) // coin
            .append(Component.text("\uF830")) // positive advance
            .append(Component.translatable("example.weather", Component.text("sunny")))
    }
}
