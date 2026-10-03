package online.libang.glyph.renderer

import online.libang.glyph.api.component.PixelComponent
import online.libang.glyph.api.player.HudPlayer
import online.libang.glyph.api.update.UpdateEvent
import online.libang.glyph.util.TickProvider

fun interface HudRenderer {
    fun render(event: UpdateEvent): TickProvider<HudPlayer, PixelComponent>
}