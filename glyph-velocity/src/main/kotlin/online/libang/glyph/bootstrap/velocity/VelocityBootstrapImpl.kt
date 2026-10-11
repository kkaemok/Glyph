package online.libang.glyph.bootstrap.velocity

import com.google.inject.Inject
import com.velocitypowered.api.command.BrigadierCommand
import com.velocitypowered.api.command.CommandSource
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.DisconnectEvent
import com.velocitypowered.api.event.player.ServerPostConnectEvent
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.plugin.PluginDescription
import com.velocitypowered.api.plugin.annotation.DataDirectory
import com.velocitypowered.api.proxy.ConsoleCommandSource
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.ProxyServer
import com.velocitypowered.api.scheduler.ScheduledTask
import online.libang.glyph.GlyphImpl
import online.libang.glyph.api.Glyph
import online.libang.glyph.api.GlyphAPI
import online.libang.glyph.api.GlyphLogger
import kr.toxicity.hud.api.adapter.LocationWrapper
import kr.toxicity.hud.api.adapter.WorldWrapper
import kr.toxicity.hud.api.player.HudPlayer
import kr.toxicity.hud.api.scheduler.HudScheduler
import kr.toxicity.hud.api.scheduler.HudTask
import kr.toxicity.hud.api.velocity.VelocityBootstrap
import kr.toxicity.hud.api.version.MinecraftVersion
import kr.toxicity.hud.api.volatilecode.VolatileCodeHandler
import online.libang.glyph.bootstrap.velocity.manager.ModuleManager
import online.libang.glyph.bootstrap.velocity.player.HudPlayerVelocity
import online.libang.glyph.manager.CommandManager
import online.libang.glyph.manager.ConfigManagerImpl
import online.libang.glyph.manager.DatabaseManagerImpl
import online.libang.glyph.manager.PlayerManagerImpl
import online.libang.glyph.pack.PackUploader
import online.libang.glyph.transport.ServerHudScope
import kr.toxicity.hud.api.plugin.ReloadState
import online.libang.glyph.util.*
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import org.bstats.velocity.Metrics
import org.bstats.velocity.Metrics.Factory
import org.slf4j.Logger
import java.io.File
import java.io.InputStream
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import java.util.UUID

@Suppress("UNUSED")
@Plugin(
    id = "glyph"
)
class VelocityBootstrapImpl @Inject constructor(
    private val proxyServer: ProxyServer,
    private val logger: Logger,
    private val factory: Factory,
    private val description: PluginDescription,
    @param:DataDirectory private val dataFolder: Path
): VelocityBootstrap, online.libang.glyph.api.GlyphBootstrap {

    private val scheduler = object : HudScheduler {
        override fun task(runnable: Runnable): HudTask {
            return proxyServer.scheduler.buildTask(this@VelocityBootstrapImpl, runnable)
                .schedule()
                .toHud()
        }
        private fun ScheduledTask.toHud() = object : HudTask {
            private var cancelled = false
            override fun isCancelled(): Boolean = cancelled
            override fun cancel() {
                cancelled = true
                this@toHud.cancel()
            }
        }

        override fun task(location: LocationWrapper, runnable: Runnable): HudTask = task(runnable)

        override fun taskLater(delay: Long, runnable: Runnable): HudTask {
            return proxyServer.scheduler.buildTask(this@VelocityBootstrapImpl, runnable)
                .delay(delay * 50, TimeUnit.MILLISECONDS)
                .schedule()
                .toHud()
        }

        override fun asyncTask(runnable: Runnable): HudTask = task(runnable)

        override fun asyncTaskLater(delay: Long, runnable: Runnable): HudTask {
            return proxyServer.scheduler.buildTask(this@VelocityBootstrapImpl, runnable)
                .delay(delay * 50, TimeUnit.MILLISECONDS)
                .schedule()
                .toHud()
        }

        override fun asyncTaskTimer(delay: Long, period: Long, runnable: Runnable): HudTask {
            return proxyServer.scheduler.buildTask(this@VelocityBootstrapImpl, runnable)
                .delay(delay * 50, TimeUnit.MILLISECONDS)
                .repeat(period * 50, TimeUnit.MILLISECONDS)
                .schedule()
                .toHud()
        }
    }

    private val log = object : GlyphLogger {
        override fun info(vararg message: String) {
            val l = logger
            synchronized(l) {
                message.forEach {
                    l.info(it)
                }
            }
        }
        override fun warn(vararg message: String) {
            val l = logger
            synchronized(l) {
                message.forEach {
                    l.warn(it)
                }
            }
        }
    }
    private val volatileCode = VelocityVolatileCodeHandler()
    private val core = GlyphImpl(this).apply {
        GlyphAPI.inst(this)
    }
    @Volatile private var ready = false
    private val hudScope = ServerHudScope<UUID>(
        { id -> proxyServer.getPlayer(id).ifPresent(::register) },
        { id, restore ->
            PlayerManagerImpl.removeHudPlayer(id)?.let { player ->
                volatileCode.removeBossBar(player, restore)
                player.cancel()
                // Finish persistence before a replacement loads it, or shutdown
                // closes the database and cancels proxy scheduler work.
                runCatching { player.save() }.handleFailure { "Unable to save ${player.name()}'s proxy HUD." }
            }
        }
    )

    private fun serverName(player: Player): String? = if (player.isActive)
        player.currentServer.orElse(null)?.serverInfo?.name else null

    private fun reconcile(player: Player) {
        hudScope.reconcile(player.uniqueId, { serverName(player) },
            { if (ready) ConfigManagerImpl.velocityHudServers else emptySet() })
    }

    private fun ownsHud(player: HudPlayer): Boolean {
        val handle = player.handle() as Player
        val server = serverName(handle)
        return ready && (server in ConfigManagerImpl.velocityHudServers || "*" in ConfigManagerImpl.velocityHudServers) &&
            hudScope.contains(handle.uniqueId, server) && PlayerManagerImpl.getHudPlayer(handle.uniqueId) === player
    }

    override fun scheduler(): HudScheduler = scheduler
    override fun jarFile(): File = File(javaClass.getProtectionDomain().codeSource.location.toURI())
    override fun core(): Glyph = core
    override fun console(): Audience = proxyServer.consoleCommandSource
    override fun volatileCode(): VolatileCodeHandler = volatileCode
    override fun version(): String = description.version.orElse("unknown")!!
    override fun resource(path: String): InputStream? = javaClass.getResourceAsStream("/$path")?.buffered()

    private var latest = emptyList<Component>()

    @Subscribe
    fun enable(e: ProxyInitializeEvent) {
        latest = handleLatestVersion()
        core.addReloadStartTask {
            ready = false
            hudScope.clear(true)
        }
        core.addReloadEndTask { result ->
            ready = result is ReloadState.Success
            proxyServer.allPlayers.forEach(::reconcile)
        }
        ModuleManager.start()
        registerCommand()
        core.start()
        scheduler.task {
            core.reload()
            log.info(
                "Platform: Velocity",
                "Plugin enabled."
            )
        }
    }


    @Subscribe
    fun disable(e: ProxyShutdownEvent) {
        ready = false
        hudScope.clear(true)
        core.end()
        metrics?.shutdown()
        log.info("Plugin disabled.")
    }

    @Subscribe
    fun connected(e: ServerPostConnectEvent) {
        reconcile(e.player)
    }

    @Subscribe
    fun disconnected(e: DisconnectEvent) {
        hudScope.disconnect(e.player.uniqueId)
    }

    private fun register(player: Player) {
        val audience = PlayerManagerImpl.addHudPlayer(player.uniqueId) {
            HudPlayerVelocity(player)
        }
        asyncTask {
            if (ownsHud(audience)) {
                DatabaseManagerImpl.currentDatabase.load(audience)
                task { sendResourcePack(audience) }
            }
        }
        if (player.hasPermission(VERSION_CHECK_PERMISSION) && ConfigManagerImpl.versionCheck) latest.forEach(audience::info)
    }

    override fun logger(): GlyphLogger = log
    override fun dataFolder(): File = File(dataFolder.toFile().parentFile, "Glyph")


    private var metrics: Metrics? = null
    override fun startMetrics() { /* Configure a Glyph metrics ID before enabling telemetry. */ }

    override fun endMetrics() {
        metrics?.shutdown()
        metrics = null
    }

    override fun sendResourcePack(player: HudPlayer) {
        if (!ownsHud(player)) return
        PackUploader.server?.let {
            (player.handle() as Player).sendResourcePackOffer(proxyServer.createResourcePackBuilder(it.url)
                .setHash(it.digest)
                  .setId(online.libang.glyph.pack.PackUUID.requestIdentity)
                .setShouldForce(false)
                .build())
        }
    }
    override fun sendResourcePack() {
        PackUploader.server?.let {
            val info = proxyServer.createResourcePackBuilder(it.url)
                .setHash(it.digest)
                .setId(online.libang.glyph.pack.PackUUID.requestIdentity)
                .setShouldForce(false)
                .build()
            PlayerManagerImpl.allHudPlayer.forEach { player ->
                if (ownsHud(player)) (player.handle() as Player).sendResourcePackOffer(info)
            }
        }
    }

    override fun minecraftVersion(): MinecraftVersion = MinecraftVersion.LATEST
    override fun mcmetaVersion(): Int = online.libang.glyph.pack.PackOverlay.entries.maxOf { it.maxVersion }

    override fun world(name: String): WorldWrapper? = null
    override fun worlds(): List<WorldWrapper> = emptyList()


    override fun classloader(): java.net.URLClassLoader {
        return javaClass.classLoader as java.net.URLClassLoader
    }

    private fun registerCommand() {
        CommandManager.module.build { s: CommandSource ->
            when (s) {
                is ConsoleCommandSource -> GlyphAPI.inst().bootstrap().consoleSource()
                is Player -> GlyphAPI.inst().playerManager.getHudPlayer(s.uniqueId)
                else -> null
            }
        }.forEach {
            BrigadierCommand(it).add()
        }
    }

    private fun BrigadierCommand.add() {
        proxyServer.commandManager.register(
            proxyServer.commandManager.metaBuilder(this).build(),
            this
        )
    }
}
