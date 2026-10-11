package online.libang.glyph.bootstrap.velocity

import com.velocitypowered.proxy.connection.client.ConnectedPlayer
import kr.toxicity.hud.api.player.HudPlayer
import kr.toxicity.hud.api.volatilecode.VolatileCodeHandler
import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.text.Component
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class VelocityVolatileCodeHandler : VolatileCodeHandler {
    companion object {
        private data class OwnedTransport(val owner: HudPlayer, val transport: VelocityBossBarTransport)
        private val bossBarMap = ConcurrentHashMap<UUID, OwnedTransport>()
    }

    override fun inject(player: HudPlayer, color: BossBar.Color) {
        val h = player.handle() as ConnectedPlayer
        bossBarMap.computeIfAbsent(h.uniqueId) {
            OwnedTransport(player, VelocityBossBarTransport(h, color, player))
        }
    }
    override fun showBossBar(player: HudPlayer, color: BossBar.Color, component: Component) {
        bossBarMap[player.uuid()]?.takeIf { it.owner === player }?.transport?.update(color, component)
    }

    override fun removeBossBar(player: HudPlayer) {
        removeBossBar(player, true)
    }

    internal fun removeBossBar(player: HudPlayer, restoreExternal: Boolean) {
        val entry = bossBarMap[player.uuid()] ?: return
        if (entry.owner === player && bossBarMap.remove(player.uuid(), entry)) entry.transport.close(restoreExternal)
    }

    override fun getTextureValue(player: HudPlayer): String {
        return (player.handle() as ConnectedPlayer).gameProfile.properties.firstOrNull {
            it.name == "textures"
        }?.value ?: ""
    }

}
