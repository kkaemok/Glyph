package online.libang.glyph.bootstrap.velocity

import com.velocitypowered.proxy.connection.client.ConnectedPlayer
import online.libang.glyph.api.player.HudPlayer
import online.libang.glyph.api.volatilecode.VolatileCodeHandler
import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.text.Component
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class VelocityVolatileCodeHandler : VolatileCodeHandler {
    companion object {
        private val bossBarMap = ConcurrentHashMap<UUID, VelocityBossBarTransport>()
    }

    override fun inject(player: HudPlayer, color: BossBar.Color) {
        val h = player.handle() as ConnectedPlayer
        bossBarMap.computeIfAbsent(h.uniqueId) {
            VelocityBossBarTransport(h, color)
        }
    }
    override fun showBossBar(player: HudPlayer, color: BossBar.Color, component: Component) {
        bossBarMap[player.uuid()]?.update(color, component)
    }

    override fun removeBossBar(player: HudPlayer) {
        bossBarMap.remove(player.uuid())?.close()
    }

    override fun getTextureValue(player: HudPlayer): String {
        return (player.handle() as ConnectedPlayer).gameProfile.properties.firstOrNull {
            it.name == "textures"
        }?.value ?: ""
    }

}
