package online.libang.glyph.bootstrap.bukkit.compatibility.craftengine

import kr.toxicity.hud.api.listener.HudListener
import kr.toxicity.hud.api.placeholder.HudPlaceholder
import kr.toxicity.hud.api.plugin.ReloadState
import kr.toxicity.hud.api.trigger.HudTrigger
import kr.toxicity.hud.api.update.UpdateEvent
import kr.toxicity.hud.api.yaml.YamlObject
import online.libang.glyph.bootstrap.bukkit.BukkitBootstrapImpl
import online.libang.glyph.bootstrap.bukkit.compatibility.Compatibility
import online.libang.glyph.bootstrap.bukkit.util.registerListener
import online.libang.glyph.manager.ConfigManagerImpl
import online.libang.glyph.pack.ExternalPackRegistration
import online.libang.glyph.pack.PackMeta
import online.libang.glyph.util.BOOTSTRAP
import online.libang.glyph.util.PLUGIN
import online.libang.glyph.util.DATA_FOLDER
import online.libang.glyph.util.handle
import online.libang.glyph.util.info
import online.libang.glyph.util.warn
import net.momirealms.craftengine.bukkit.api.event.AsyncResourcePackCacheEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

class CraftEngineCompatibility : Compatibility {
    private val registration by lazy {
        ExternalPackRegistration(DATA_FOLDER.toPath().resolve(".cache/craftengine-registration.zip"))
    }
    override val website: String = "https://polymart.org/product/7624/craftengine"
    override val triggers: Map<String, (YamlObject) -> HudTrigger<*>> = mapOf()
    override val listeners: Map<String, (YamlObject) -> (UpdateEvent) -> HudListener> = mapOf()
    override val numbers: Map<String, HudPlaceholder<Number>> = mapOf()
    override val strings: Map<String, HudPlaceholder<String>> = mapOf()
    override val booleans: Map<String, HudPlaceholder<Boolean>> = mapOf()

    override fun start() {
        ConfigManagerImpl.preReload()
        if (ConfigManagerImpl.mergeWithExternalResources) (BOOTSTRAP as BukkitBootstrapImpl).skipInitialReload = true
        registerListener(object : Listener {
            @EventHandler
            @Synchronized
            fun AsyncResourcePackCacheEvent.generate() {
                if (!ConfigManagerImpl.mergeWithExternalResources) return
                when (val result = PLUGIN.reload()) {
                    is ReloadState.Success -> {
                        result.directory()?.let {
                            val cache = cacheData()
                            val nativePacks = net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine.instance()
                                .packManager().loadedPacks().filter { pack -> pack.enabled() }
                                .flatMap { pack -> pack.resourcePackFolders().toList() }
                            val existing = nativePacks + cache.externalFolders() + cache.externalZips()
                            try {
                                val inspection = registration.register(it.toPath(), existing, cache.externalFolders(), cache.externalZips(),
                                    PackMeta.VersionFormat(BOOTSTRAP.mcmetaVersion(), BOOTSTRAP.mcmetaMinorVersion()))
                                if (inspection.textureOwners.isNotEmpty()) {
                                    warn("External texture owners selected; Glyph's CraftEngine copy omits these duplicates: " +
                                        inspection.textureOwners.take(4).joinToString { owner -> "${owner.resource} (${owner.owner})" } +
                                        if (inspection.textureOwners.size > 4) " and ${inspection.textureOwners.size - 4} more." else ".")
                                }
                                if (inspection.equivalentTextures.isNotEmpty()) {
                                    info("Compatible texture collisions have equivalent pixels: " +
                                        inspection.equivalentTextures.take(4).joinToString())
                                }
                            } catch (conflict: IllegalArgumentException) {
                                conflict.handle("Glyph/CraftEngine pack conflict; Glyph assets were not registered. Resolve shader/font/post-effect ownership or incompatible overlays.")
                                return
                            }
                            info("Successfully merged with CraftEngine.")
                        }
                    }
                    is ReloadState.OnReload -> {
                        warn("Glyph is still on reload!")
                    }
                    is ReloadState.Failure -> {
                        result.throwable.handle("Unable to merge with CraftEngine.")
                    }
                }
            }
        })
    }
}
