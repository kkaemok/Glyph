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
        assertEquals("<red>Zombie", "짠cZombie".toMiniMessageString())
    }

    @Test
    fun hexColorCodeIsConverted() {
        assertEquals("<#FFD800>Test", "짠x짠F짠F짠D짠8짠0짠0Test".toMiniMessageString())
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
        val input = "짠cCustom 짠lName"
        val result = input.toMiniMessageString()
        val expected = LEGACY_SECTION_SERIALIZER.deserialize(input)
        assertEquals(expected, MiniMessage.miniMessage().deserialize(result))
    }
}