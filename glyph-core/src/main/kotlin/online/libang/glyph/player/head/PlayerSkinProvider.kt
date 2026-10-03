package online.libang.glyph.player.head

import online.libang.glyph.api.player.HudPlayer

interface PlayerSkinProvider {
    fun provide(player: HudPlayer): String?
    fun provide(playerName: String): String?
}