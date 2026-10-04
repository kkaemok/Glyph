package online.libang.glyph

import online.libang.glyph.api.Glyph
import online.libang.glyph.api.GlyphBootstrap
import kr.toxicity.hud.api.manager.*
import kr.toxicity.hud.api.plugin.ReloadInfo
import kr.toxicity.hud.api.plugin.ReloadState
import kr.toxicity.hud.api.plugin.ReloadState.Failure
import kr.toxicity.hud.api.plugin.ReloadState.Success
import online.libang.glyph.manager.*
import online.libang.glyph.pack.PackGenerator
import online.libang.glyph.pack.PackUploader
import online.libang.glyph.resource.GlobalResource
import online.libang.glyph.util.*
import net.kyori.adventure.key.Key
import java.io.File
import java.io.InputStream
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.BiConsumer
import java.util.function.Consumer
import java.util.jar.Attributes
import java.util.jar.JarFile
import java.util.jar.Manifest
import java.util.regex.Pattern
import java.util.zip.ZipEntry

@Suppress("UNUSED")
class GlyphImpl(val bootstrap: GlyphBootstrap) : Glyph {

    init {
        if (!bootstrap.dataFolder().exists() && !bootstrap.isVelocity) loadAssets("default", bootstrap.dataFolder().apply {
            mkdir()
        })

    }

    private val managers: List<GlyphManager> by lazy {
        listOf(
            ConfigManagerImpl,
            MinecraftManager,
            CommandManager,
            DatabaseManagerImpl,

            ListenerManagerImpl,
            PlaceholderManagerImpl,
            TriggerManagerImpl,

            BackgroundManager,
            ImageManager,
            TextManagerImpl,
            PlayerHeadManager,
            LayoutManager,
            HudManagerImpl,
            PopupManagerImpl,
            CompassManagerImpl,

            ShaderManagerImpl,
            PlayerManagerImpl,
            EncodeManager,
        )
    }

    fun start() {
        managers.forEach {
            it.start()
        }
    }

    private val reloadStartTask = ArrayList<() -> Unit>()
    private val reloadEndTask = ArrayList<(ReloadState) -> Unit>()
    private val isDevVersion = JarFile(bootstrap.jarFile()).use {
        it.getInputStream(ZipEntry("META-INF/MANIFEST.MF"))?.buffered()?.use { stream ->
            Manifest(stream).mainAttributes.getValue(Attributes.Name("Dev-Build"))?.toBoolean()
        }
    } == true

    override fun addReloadStartTask(runnable: Runnable) {
        reloadStartTask += {
            runnable.run()
        }
    }

    override fun addReloadEndTask(runnable: Consumer<ReloadState>) {
        reloadEndTask += {
            runnable.accept(it)
        }
    }

    private val onReload = AtomicBoolean()

    override fun reload(info: ReloadInfo): ReloadState {
        if (!onReload.compareAndSet(false, true)) return ReloadState.ON_RELOAD
        val time = System.currentTimeMillis()
        reloadStartTask.forEach {
            it()
        }
        val result = runCatching {
            managers.forEach {
                it.preReload()
            }
            val resource = GlobalResource(info)
            DATA_FOLDER.subFolder("packs").forEach { pack ->
                if (!pack.isDirectory || pack.name.startsWith('-')) return@forEach
                managers.filter {
                    it.supportExternalPacks
                }.forEach {
                    debug(ConfigManager.DebugLevel.MANAGER, "Reloading ${it.managerName} in ${pack.name}...")
                    it.reload(pack, info, resource)
                }
            }
            managers.forEach {
                debug(ConfigManager.DebugLevel.MANAGER, "Reloading ${it.managerName}...")
                it.reload(DATA_FOLDER, info, resource)
            }
            managers.forEach {
                it.postReload()
            }
            PackGenerator.generate(info).run {
                Success(
                    this,
                    ConfigManagerImpl.packType.generate(this),
                    System.currentTimeMillis() - time
                )
            }
        }.getOrElse {
            it.handle(info.sender, "Unable to reload.")
            Failure(it)
        }
        reloadEndTask.forEach {
            it(result)
        }
        onReload.set(false)
        return result
    }

    override fun bootstrap(): GlyphBootstrap = bootstrap


    fun end() {
        PackUploader.stop()
        managers.forEach {
            it.end()
        }
        BOOTSTRAP.endMetrics()
        DatabaseManagerImpl.currentDatabase.close()
    }

    override fun getWidth(codepoint: Int): Int = TextManagerImpl.getWidth(codepoint)

    override fun loadAssets(prefix: String, dir: File) {
        loadAssets(prefix) { s, i ->
            File(dir, s).apply {
                parentFile.mkdirs()
            }.outputStream().buffered().use {
                i.copyTo(it)
            }
        }
    }

    override fun loadAssets(prefix: String, consumer: BiConsumer<String, InputStream>) {
        loadAssets(prefix) { s, i ->
            consumer.accept(s, i)
        }
    }

    private fun loadAssets(prefix: String, consumer: (String, InputStream) -> Unit) {
        JarFile(bootstrap.jarFile()).use {
            it.entries().asSequence().forEach { entry ->
                if (!entry.name.startsWith(prefix)) return@forEach
                if (entry.name.length <= prefix.length + 1) return@forEach
                val name = entry.name.substring(prefix.length + 1)
                if (!entry.isDirectory) it.getInputStream(entry).buffered().use { stream ->
                    consumer(name, stream)
                }
            }
        }
    }

    private val numberPattern = Pattern.compile("[0-9]+")
    private fun String.extractNumber() = split('.').map {
        val matcher = numberPattern.matcher(it)
        buildString {
            while (matcher.find()) append(matcher.group())
        }.toInt()
    }

    override fun getEncodedNamespace(): String = NAME_SPACE_ENCODED
    override fun isMergeBossBar(): Boolean = ConfigManagerImpl.mergeBossBar
    override fun getPlaceholderManager(): PlaceholderManager = PlaceholderManagerImpl
    override fun getListenerManager(): ListenerManager = ListenerManagerImpl
    override fun getPopupManager(): PopupManager = PopupManagerImpl
    override fun getTriggerManager(): TriggerManager = TriggerManagerImpl
    override fun getHudManager(): HudManager = HudManagerImpl
    override fun getDatabaseManager(): DatabaseManager = DatabaseManagerImpl
    override fun getShaderManager(): ShaderManager = ShaderManagerImpl
    override fun getCompassManager(): CompassManager = CompassManagerImpl
    override fun getConfigManager(): ConfigManager = ConfigManagerImpl
    override fun getPlayerManager(): PlayerManager = PlayerManagerImpl
    override fun getTextManager(): TextManager = TextManagerImpl
    override fun isOnReload(): Boolean = onReload.get()
    override fun getDefaultKey(): Key = DEFAULT_KEY
    override fun translate(locale: String, key: String): String? = TextManagerImpl.translate(locale, key)
    override fun isDevVersion(): Boolean = isDevVersion

}