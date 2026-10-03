package online.libang.glyph.bootstrap.bukkit

import online.libang.glyph.GlyphImpl
import online.libang.glyph.api.Glyph
import online.libang.glyph.api.GlyphAPI
import online.libang.glyph.api.GlyphLogger
import online.libang.glyph.api.adapter.WorldWrapper
import online.libang.glyph.api.bukkit.BukkitBootstrap
import online.libang.glyph.api.bukkit.bedrock.BedrockAdapter
import online.libang.glyph.api.bukkit.event.HudPlayerJoinEvent
import online.libang.glyph.api.bukkit.event.HudPlayerQuitEvent
import online.libang.glyph.api.bukkit.event.PluginReloadStartEvent
import online.libang.glyph.api.bukkit.event.PluginReloadedEvent
import online.libang.glyph.api.bukkit.nms.NMS
import online.libang.glyph.api.manager.ConfigManager
import online.libang.glyph.api.placeholder.HudPlaceholder
import online.libang.glyph.api.player.HudPlayer
import online.libang.glyph.api.scheduler.HudScheduler
import online.libang.glyph.api.version.MinecraftVersion
import online.libang.glyph.bedrock.FloodgateAdapter
import online.libang.glyph.bedrock.GeyserAdapter
import online.libang.glyph.bootstrap.bukkit.manager.CompatibilityManager
import online.libang.glyph.bootstrap.bukkit.manager.ModuleManager
import online.libang.glyph.bootstrap.bukkit.player.HudPlayerBukkit
import online.libang.glyph.bootstrap.bukkit.player.head.SkinsRestorerSkinProvider
import online.libang.glyph.bootstrap.bukkit.player.location.GPSLocationProvider
import online.libang.glyph.bootstrap.bukkit.util.bukkitPlayer
import online.libang.glyph.bootstrap.bukkit.util.call
import online.libang.glyph.bootstrap.bukkit.util.registerListener
import online.libang.glyph.manager.*
import online.libang.glyph.pack.PackUploader
import online.libang.glyph.placeholder.PlaceholderTask
import online.libang.glyph.player.head.HttpSkinProvider
import online.libang.glyph.player.head.MineToolsProvider
import online.libang.glyph.scheduler.PaperScheduler
import online.libang.glyph.util.*
import me.clip.placeholderapi.PlaceholderAPI
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import org.bstats.bukkit.Metrics
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.server.ServerLoadEvent
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.io.InputStream
import java.util.function.Function

@Suppress("UNUSED")
class BukkitBootstrapImpl : BukkitBootstrap, JavaPlugin() {

    private val listener = object : Listener {}

    private val isFolia = runCatching {
        Class.forName("io.papermc.paper.threadedregions.RegionizedServer")
        true
    }.getOrDefault(false)

    private val scheduler = PaperScheduler(this)
    private val updateTask = java.util.concurrent.CopyOnWriteArrayList<PlaceholderTask>()
    private val minecraftVersion by lazy {
        Bukkit.getBukkitVersion()
            .substringBefore('-')
            .toMinecraftVersion()
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
                    l.warning(it)
                }
            }
        }
    }

    private val core = GlyphImpl(this).apply {
        GlyphAPI.inst(this)
        addReloadStartTask {
            scheduler.asyncTask {
                PluginReloadStartEvent().call()
            }
        }
        addReloadEndTask {
            scheduler.asyncTask {
                PluginReloadedEvent(it).call()
            }
        }
        addReloadStartTask {
            HandlerList.unregisterAll(listener)
        }
        addReloadEndTask {
            updateTask.clear()
            if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                DATA_FOLDER.subFolder("placeholders").forEachAllYaml(CONSOLE) { file, s, yamlObject ->
                    runCatching {
                        val variable = yamlObject["variable"]?.asString().ifNull { "variable not set." }
                        val placeholder = yamlObject["placeholder"]?.asString().ifNull { "placeholder not set." }
                        val update = yamlObject.getAsInt("update", 1).coerceAtLeast(1)
                        val async = yamlObject.getAsBoolean("async", false)
                        updateTask.add(object : PlaceholderTask {
                            override val tick: Int = update
                            override val async: Boolean = async

                            override fun invoke(p1: HudPlayer) {
                                runCatching {
                                    p1.variableMap[variable] = PlaceholderAPI.setPlaceholders(p1.bukkitPlayer, placeholder)
                                }
                            }
                        })
                    }.getOrElse {
                        it.handle("Unable to read this placeholder task: $s in ${file.name}")
                    }
                }
            }
        }
    }

    // Render updates run on the owning entity scheduler. No location-based rescheduling.
    fun update(player: HudPlayer) {
        for (work in updateTask) {
            if (player.tick % work.tick != 0L) continue
            if (work.async) scheduler.asyncTask { work(player) } else work(player)
        }
    }

    private lateinit var nms: NMS

    private val bedrockAdapter by lazy {
        Bukkit.getPluginManager().run {
            if (isPluginEnabled("Geyser-Spigot")) {
                GeyserAdapter()
            } else if (isPluginEnabled("floodgate")) {
                FloodgateAdapter()
            } else BedrockAdapter { false }
        }
    }

    override fun isFolia(): Boolean = isFolia
    override fun volatileCode(): NMS = nms
    override fun bedrockAdapter(): BedrockAdapter = bedrockAdapter

    override fun isPaper(): Boolean = true
    override fun scheduler(): HudScheduler = scheduler
    override fun jarFile(): File = file
    override fun core(): Glyph = core
    override fun version(): String = description.version

    var skipInitialReload = false

    override fun onLoad() {
        val pluginManager = Bukkit.getPluginManager()
        nms = when (minecraftVersion) {
            MinecraftVersion.V26_3 -> online.libang.glyph.nms.v26_R2.NMSImpl(online.libang.glyph.api.bukkit.nms.NMSVersion.V26_R3)
            MinecraftVersion.V26_2 -> online.libang.glyph.nms.v26_R2.NMSImpl()
            MinecraftVersion.V26_1, MinecraftVersion.V26_1_1, MinecraftVersion.V26_1_2 -> online.libang.glyph.nms.v26_R1.NMSImpl()
            else -> {
                warn("Unsupported minecraft version: $minecraftVersion")
                pluginManager.disablePlugin(this)
                return
            }
        }
        nms.registerCommand(CommandManager.module)
    }

    private var latest = emptyList<Component>()

    override fun onEnable() {
        nms.handleReloadCommand(CommandManager.module)
        val pluginManager = Bukkit.getPluginManager()
        if (pluginManager.isPluginEnabled("GPS")) PlayerManagerImpl.addLocationProvider(GPSLocationProvider())
        pluginManager.registerEvents(object : Listener {
            @EventHandler(priority = EventPriority.HIGHEST)
            fun PlayerJoinEvent.join() {
                register(player)
            }
            @EventHandler
            fun PlayerQuitEvent.quit() {
                val player = player
                PlayerManagerImpl.removeHudPlayer(player.uniqueId)?.let {
                    it.cancel()
                    HudPlayerQuitEvent(it).call()
                    asyncTask {
                        it.save()
                    }
                }
            }
        }, this)
        if (Bukkit.getPluginManager().isPluginEnabled("SkinsRestorer")) {
            PlayerHeadManager.addSkinProvider(SkinsRestorerSkinProvider())
        }
        if (!Bukkit.getServer().onlineMode) {
            PlayerHeadManager.addSkinProvider(MineToolsProvider())
            PlayerHeadManager.addSkinProvider(HttpSkinProvider())
        }
        if (pluginManager.isPluginEnabled("PlaceholderAPI")) {
            PlaceholderManagerImpl.stringContainer.addPlaceholder("papi", HudPlaceholder.builder<String>()
                .requiredArgsLength(1)
                .function { args, _ ->
                    val format = "%${args.joinToString(",")}%"
                    Function { player ->
                        runCatching {
                            PlaceholderAPI.setPlaceholders(player.bukkitPlayer, format)
                        }.getOrDefault("<error>")
                    }
                }
                .build())
        }
        ModuleManager.start()
        CompatibilityManager.start()
        Bukkit.getOnlinePlayers().forEach {
            register(it)
        }
        latest = handleLatestVersion()
        core.start()
        registerListener(object : Listener {
            @EventHandler
            fun ServerLoadEvent.load() {
                debug(ConfigManager.DebugLevel.MANAGER,"Initialized: $type")
                if (!skipInitialReload) {
                    core.reload()
                }
                log.info(
                    "Minecraft version: $minecraftVersion, NMS version: ${nms.version}",
                    "Platform: ${when {
                        isFolia -> "Folia"
                        else -> "Paper"
                    }}",
                    "Plugin enabled."
                )
            }
        })
    }

    override fun onDisable() {
        core.end()
        metrics?.shutdown()
        log.info("Plugin disabled.")
    }

    fun register(player: Player) {
        if (!player.isOnline) return
        if (ConfigManagerImpl.disableToBedrockPlayer && bedrockAdapter.isBedrockPlayer(player.uniqueId)) return
        val audience = PlayerManagerImpl.addHudPlayer(player.uniqueId) {
            val impl = HudPlayerBukkit(player, player)
            asyncTask {
                DatabaseManagerImpl.currentDatabase.load(impl)
                player.scheduler.run(this, {
                    sendResourcePack(impl)
                    player.updateCommands()
                    HudPlayerJoinEvent(impl).call()
                }, null)
            }
            impl
        }
        if (player.hasPermission(VERSION_CHECK_PERMISSION) && ConfigManagerImpl.versionCheck) latest.forEach(audience::info)
    }

    override fun resource(path: String): InputStream? = getResource(path)
    override fun logger(): GlyphLogger = log
    override fun dataFolder(): File = dataFolder
    override fun console(): Audience = Bukkit.getConsoleSender()

    private var metrics: Metrics? = null
    override fun startMetrics() { /* Configure a Glyph metrics ID before enabling telemetry. */ }

    override fun endMetrics() {
        metrics?.shutdown()
        metrics = null
    }

    override fun sendResourcePack(player: HudPlayer) {
        requestPack(player.handle() as Player)
    }
    private fun requestPack(player: Player) {
        val pack = PackUploader.server ?: return
        val identity = online.libang.glyph.pack.PackUUID.requestIdentity
        val request = net.kyori.adventure.resource.ResourcePackRequest.resourcePackRequest()
            .packs(net.kyori.adventure.resource.ResourcePackInfo.resourcePackInfo(identity, java.net.URI.create(pack.url), pack.digestString))
            .replace(false).required(false).build()
        player.scheduler.run(this, { player.sendResourcePacks(request) }, null)
    }
    override fun postEffects(player: HudPlayer): online.libang.glyph.api.effect.PostEffects =
        if (minecraftVersion >= MinecraftVersion.V26_3)
            online.libang.glyph.bootstrap.bukkit.effect.PaperPostEffects(player.handle() as Player)
        else online.libang.glyph.api.effect.PostEffects.unsupported()
    override fun sendResourcePack() {
        Bukkit.getOnlinePlayers().forEach(::requestPack)
    }

    override fun minecraftVersion(): MinecraftVersion = minecraftVersion
    override fun mcmetaVersion(): Int = nms.version.metaVersion
    override fun mcmetaMinorVersion(): Int = nms.version.metaMinorVersion
    override fun triggerListener(): Listener = listener

    override fun world(name: String): WorldWrapper? {
        return Bukkit.getWorld(name)?.let {
            WorldWrapper(it.name)
        }
    }

    override fun worlds(): List<WorldWrapper> = Bukkit.getWorlds().map {
        WorldWrapper(it.name)
    }

    override fun classloader(): ClassLoader {
        return javaClass.classLoader
    }
}
