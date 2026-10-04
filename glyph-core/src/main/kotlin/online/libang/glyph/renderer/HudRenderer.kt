package online.libang.glyph.renderer

import kr.toxicity.hud.api.component.PixelComponent
import kr.toxicity.hud.api.player.HudPlayer
import kr.toxicity.hud.api.update.UpdateEvent
import online.libang.glyph.util.TickProvider

fun interface HudRenderer {
    fun render(event: UpdateEvent): TickProvider<HudPlayer, PixelComponent>
}