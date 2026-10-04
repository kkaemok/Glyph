package online.libang.glyph.hud

import kr.toxicity.hud.api.component.PixelComponent
import kr.toxicity.hud.api.player.HudPlayer

fun interface HudSubParser {
    fun render(player: HudPlayer): (Long) -> PixelComponent
}