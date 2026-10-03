package online.libang.glyph.util

import net.kyori.adventure.text.Component

const val VERSION_CHECK_PERMISSION = "glyph.info"
// Glyph has no published release feed yet. Never advertise BetterHud artifacts as Glyph updates.
fun handleLatestVersion(): List<Component> = emptyList()
