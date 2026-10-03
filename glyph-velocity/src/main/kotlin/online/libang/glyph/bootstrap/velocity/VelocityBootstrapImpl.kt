package online.libang.glyph.bootstrap.velocity

import com.google.inject.Inject
import com.velocitypowered.api.command.BrigadierCommand
import com.velocitypowered.api.command.CommandSource
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.PostLoginEvent
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
import online.libang.glyph.api.adapter.LocationWrapper
import online.libang.glyph.api.adapter.WorldWrapper
import online.libang.glyph.api.player.HudPlayer
import online.libang.glyph.api.scheduler.HudScheduler
import online.libang.glyph.api.scheduler.HudTask
import online.libang.glyph.api.velocity.VelocityBootstrap
import online.libang.glyph.api.version.MinecraftVersion
import online.libang.glyph.api.volatilecode.VolatileCodeHandler
import online.libang.glyph.bootstrap.velocity.manager.ModuleManager
import online.libang.glyph.bootstrap.velocity.player.HudPlayerVelocity
import online.libang.glyph.manager.CommandManager
import online.libang.glyph.manager.ConfigManagerImpl
import online.libang.glyph.manager.DatabaseManagerImpl
import online.libang.glyph.manager.PlayerManagerImpl
import online.libang.glyph.pack.PackUploader
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
): VelocityBootstrap {

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
        proxyServer.allPlayers.forEach {
            register(it)
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
        core.end()
        metrics?.shutdown()
        log.info("Plugin disabled.")
    }

    @Subscribe
    fun login(e: PostLoginEvent) {
        register(e.player)
    }

    private fun register(player: Player) {
        val audience = PlayerManagerImpl.addHudPlayer(player.uniqueId) {
            val impl = HudPlayerVelocity(player)
            asyncTask {
                DatabaseManagerImpl.currentDatabase.load(impl)
                task {
                    sendResourcePack(impl)
                }
            }
            impl
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
        PackUploader.server?.let {
            (player.handle() as Player).sendResourcePackOffer(proxyServer.createResourcePackBuilder(it.url)
                .setHash(it.digest)
                .setId(it.uuid)
                .setShouldForce(true)
                .build())
        }
    }
    override fun sendResourcePack() {
        PackUploader.server?.let {
            val info = proxyServer.createResourcePackBuilder(it.url)
                .setHash(it.digest)
                .setId(it.uuid)
                .setShouldForce(true)
                .build()
            proxyServer.allServers.forEach { p ->
                p.sendResourcePacks(info)
            }
        }
    }

    override fun minecraftVersion(): MinecraftVersion = MinecraftVersion.LATEST
    override fun mcmetaVersion(): Int = 84

    override fun world(name: String): WorldWrapper? = null
    override fun worlds(): List<WorldWrapper> = emptyList()


    override fun classloader(): ClassLoader {
        return javaClass.classLoader
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