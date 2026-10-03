package online.libang.glyph.player.head

import online.libang.glyph.api.player.HudPlayer
import online.libang.glyph.manager.PlayerManagerImpl
import online.libang.glyph.util.textures

class GameProfileSkinProvider : PlayerSkinProvider {
    override fun provide(player: HudPlayer): String {
        return player.textures
    }

    override fun provide(playerName: String): String {
        return PlayerManagerImpl.getHudPlayer(playerName)?.textures ?: ""
    }
}