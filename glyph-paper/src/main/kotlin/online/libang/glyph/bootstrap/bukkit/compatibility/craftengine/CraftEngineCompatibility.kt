package online.libang.glyph.bootstrap.bukkit.compatibility.craftengine

import online.libang.glyph.api.listener.HudListener
import online.libang.glyph.api.placeholder.HudPlaceholder
import online.libang.glyph.api.plugin.ReloadState
import online.libang.glyph.api.trigger.HudTrigger
import online.libang.glyph.api.update.UpdateEvent
import online.libang.glyph.api.yaml.YamlObject
import online.libang.glyph.bootstrap.bukkit.BukkitBootstrapImpl
import online.libang.glyph.bootstrap.bukkit.compatibility.Compatibility
import online.libang.glyph.bootstrap.bukkit.util.registerListener
import online.libang.glyph.manager.ConfigManagerImpl
import online.libang.glyph.util.BOOTSTRAP
import online.libang.glyph.util.PLUGIN
import online.libang.glyph.util.handle
import online.libang.glyph.util.info
import online.libang.glyph.util.warn
import net.momirealms.craftengine.bukkit.api.event.AsyncResourcePackCacheEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

class CraftEngineCompatibility : Compatibility {
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
            fun AsyncResourcePackCacheEvent.generate() {
                if (!ConfigManagerImpl.mergeWithExternalResources) return
                when (val result = PLUGIN.reload()) {
                    is ReloadState.Success -> {
                        result.directory()?.let {
                            when {
                                it.isFile -> cacheData().externalZips().add(it.toPath())
                                it.isDirectory -> cacheData().externalFolders().add(it.toPath())
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