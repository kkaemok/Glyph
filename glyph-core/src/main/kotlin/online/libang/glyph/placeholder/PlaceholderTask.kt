package online.libang.glyph.placeholder

import online.libang.glyph.api.player.HudPlayer

interface PlaceholderTask : (HudPlayer) -> Unit {
    val tick: Int
    val async: Boolean
}