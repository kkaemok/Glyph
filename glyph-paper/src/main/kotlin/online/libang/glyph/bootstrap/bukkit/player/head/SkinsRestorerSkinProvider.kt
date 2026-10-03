package online.libang.glyph.bootstrap.bukkit.player.head

import online.libang.glyph.api.player.HudPlayer
import online.libang.glyph.player.head.PlayerSkinProvider
import net.skinsrestorer.api.SkinsRestorerProvider
import org.bukkit.Bukkit
import java.util.*

class SkinsRestorerSkinProvider : PlayerSkinProvider {
    override fun provide(player: HudPlayer): String? {
        return player.uuid().provide()
    }

    override fun provide(playerName: String): String? {
        @Suppress("DEPRECATION") //It is not deprecated in Paper.
        return Bukkit.getServer().getOfflinePlayer(playerName).uniqueId.provide()
    }

    private fun UUID.provide(): String? {
        return SkinsRestorerProvider.get().playerStorage.getSkinOfPlayer(this).map {
            it.value
        }.orElse(null)
    }
}