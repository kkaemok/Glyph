package online.libang.glyph.player.head

import kr.toxicity.hud.api.player.HudPlayer

interface PlayerSkinProvider {
    fun provide(player: HudPlayer): String?
    fun provide(playerName: String): String?
}