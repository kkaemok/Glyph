package online.libang.glyph.hud

import online.libang.glyph.api.component.PixelComponent
import online.libang.glyph.api.player.HudPlayer

fun interface HudSubParser {
    fun render(player: HudPlayer): (Long) -> PixelComponent
}