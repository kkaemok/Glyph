package online.libang.glyph.util

import online.libang.glyph.util.LEGACY_SECTION_SERIALIZER
import net.kyori.adventure.text.minimessage.MiniMessage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class NameSerializerTest {

    @Test
    fun plainNameIsUnchanged() {
        assertEquals("Zombie", "Zombie".toMiniMessageString())
    }

    @Test
    fun legacyColorCodeIsConverted() {
        assertEquals("<red>Zombie", "\u00a7cZombie".toMiniMessageString())
    }

    @Test
    fun hexColorCodeIsConverted() {
        assertEquals("<#FFD800>Test", "\u00a7x\u00a7F\u00a7F\u00a7D\u00a78\u00a70\u00a70Test".toMiniMessageString())
    }

    @Test
    fun miniMessageSpecialCharactersAreEscaped() {
        // MiniMessage escapes the tag-opening '<' ("\\<red>"); a bare '>' is inert to the parser.
        assertEquals("\\<red>", "<red>".toMiniMessageString())
    }

    @Test
    fun miniMessageSpecialCharactersRoundTrip() {
        val input = "<red>"
        val reparsed = MiniMessage.miniMessage().deserialize(input.toMiniMessageString())
        assertEquals(LEGACY_SECTION_SERIALIZER.deserialize(input), reparsed)
    }

    @Test
    fun conversionRoundTripsThroughMiniMessage() {
        val input = "\u00a7cCustom \u00a7lName"
        val result = input.toMiniMessageString()
        val expected = LEGACY_SECTION_SERIALIZER.deserialize(input)
        assertEquals(expected, MiniMessage.miniMessage().deserialize(result))
    }

    @Test
    fun koreanTextIsNotMistakenForFormatting() {
        assertEquals("짠cZombie", "짠cZombie".toMiniMessageString())
    }

    @Test
    fun combinedSerializerAcceptsBothActualLegacyPrefixes() {
        val parsed = LEGACY_BOTH("&cRed \u00a7lBold")
        val expected = net.kyori.adventure.text.Component.text("Red ", net.kyori.adventure.text.format.NamedTextColor.RED)
            .append(net.kyori.adventure.text.Component.text("Bold")
                .decorate(net.kyori.adventure.text.format.TextDecoration.BOLD))
        assertEquals(expected, parsed)
    }
}
