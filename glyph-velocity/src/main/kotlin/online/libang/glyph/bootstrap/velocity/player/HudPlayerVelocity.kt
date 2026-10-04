package online.libang.glyph.bootstrap.velocity.player

import com.velocitypowered.api.proxy.Player
import kr.toxicity.hud.api.adapter.LocationWrapper
import kr.toxicity.hud.api.adapter.WorldWrapper
import online.libang.glyph.manager.PlayerManagerImpl
import online.libang.glyph.player.HudPlayerImpl
import online.libang.glyph.util.asyncTask
import net.kyori.adventure.audience.Audience
import java.util.*

class HudPlayerVelocity(
    private val player: Player,
) : HudPlayerImpl() {
    override fun uuid(): UUID = player.uniqueId
    override fun name(): String = player.username
    override fun handle(): Any = player
    override fun audience(): Audience = player

    override fun world(): WorldWrapper = throw UnsupportedOperationException("velocity")
    override fun location(): LocationWrapper = throw UnsupportedOperationException("velocity")
    override fun updatePlaceholder() {
        if (!player.isActive) {
            PlayerManagerImpl.removeHudPlayer(player.uniqueId)?.let {
                it.cancel()
                asyncTask {
                    it.save()
                }
            }
        }
    }

    init {
        inject()
    }

    override fun locale(): Locale = player.effectiveLocale ?: Locale.US

    override fun hasPermission(perm: String): Boolean = player.hasPermission(perm)
}