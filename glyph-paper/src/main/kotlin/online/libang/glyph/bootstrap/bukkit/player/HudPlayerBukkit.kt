package online.libang.glyph.bootstrap.bukkit.player

import online.libang.glyph.api.adapter.LocationWrapper
import online.libang.glyph.api.adapter.WorldWrapper
import online.libang.glyph.bootstrap.bukkit.BukkitBootstrapImpl
import online.libang.glyph.player.HudPlayerImpl
import online.libang.glyph.util.BOOTSTRAP
import online.libang.glyph.util.asyncTaskLater
import net.kyori.adventure.audience.Audience
import org.bukkit.Bukkit
import org.bukkit.boss.BossBar
import org.bukkit.entity.Player
import java.util.*

class HudPlayerBukkit(
    private val player: Player,
    private val audience: Audience
) : HudPlayerImpl() {
    override fun uuid(): UUID = player.uniqueId
    override fun name(): String = player.name
    override fun handle(): Any = player
    override fun audience(): Audience = audience
    override fun world(): WorldWrapper = WorldWrapper(
        player.world.name
    )

    override fun locale(): Locale = player.locale.let {
        val split = it.split('_')
        if (split.size == 1) Locale.of(split[0].lowercase()) else Locale.of(split[0].lowercase(), split[1].uppercase())
    }

    override fun location(): LocationWrapper {

        val loc = player.location
        return LocationWrapper(
            world(),
            loc.x,
            loc.y,
            loc.z,
            loc.pitch,
            loc.yaw
        )
    }

    override fun scheduleOwned(period: Long, action: () -> Unit): online.libang.glyph.api.scheduler.HudTask {
        val scheduled = player.scheduler.runAtFixedRate(BOOTSTRAP as org.bukkit.plugin.Plugin,
            { action() }, null, 1, period)
        return object : online.libang.glyph.api.scheduler.HudTask {
            override fun isCancelled(): Boolean = scheduled == null || scheduled.isCancelled
            override fun cancel() { scheduled?.cancel() }
        }
    }

    override fun updatePlaceholder() {
        (BOOTSTRAP as BukkitBootstrapImpl).update(this)
    }

    override fun reload() {
        initBossBar {
            super.reload()
        }
    }

    init {
        initBossBar {
            inject()
        }
    }

    private fun initBossBar(action: () -> Unit) {
        player.scheduler.run(BOOTSTRAP as org.bukkit.plugin.Plugin, { initBossBarOwned(action) }, null)
    }

    private fun initBossBarOwned(action: () -> Unit) {
        val bars = ArrayList<BossBar>()
        for (bossBar in Bukkit.getBossBars()) {
            if (bossBar.players.any {
                it.uniqueId == player.uniqueId
            }) {
                bossBar.removePlayer(player)
                bars += bossBar
            }
        }
        action()
        player.scheduler.runDelayed(BOOTSTRAP as org.bukkit.plugin.Plugin, {
            bars.forEach { it.addPlayer(player) }
        }, null, 20)
    }

    override fun hasPermission(perm: String): Boolean = player.hasPermission(perm)
}
