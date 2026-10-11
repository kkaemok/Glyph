package online.libang.glyph.util

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer

fun interface ComponentDeserializer : (String) -> Component

val LEGACY_SECTION_SERIALIZER by lazy {
    LegacyComponentSerializer.builder()
        .useUnusualXRepeatedCharacterHexFormat()
        .character('\u00a7')
        .extractUrls()
        .hexColors()
        .build()
}
val LEGACY_AMPERSAND_SERIALIZER by lazy {
    LegacyComponentSerializer.builder()
        .useUnusualXRepeatedCharacterHexFormat()
        .character('&')
        .extractUrls()
        .hexColors()
        .build()
}

val LEGACY_SECTION: ComponentDeserializer = ComponentDeserializer { p1 -> LEGACY_SECTION_SERIALIZER.deserialize(p1) }
val LEGACY_AMPERSAND: ComponentDeserializer = ComponentDeserializer { p1 -> LEGACY_AMPERSAND_SERIALIZER.deserialize(p1) }
val LEGACY_BOTH: ComponentDeserializer = ComponentDeserializer { p1 -> LEGACY_SECTION_SERIALIZER.deserialize(p1.replace('&', '\u00a7')) }
/** Safe legacy names at the boundary of the string placeholder API. */
fun legacyNameToMiniMessage(value: String) = net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
    .serialize(LEGACY_SECTION_SERIALIZER.deserialize(value))
fun String.toMiniMessageString() = legacyNameToMiniMessage(this)
