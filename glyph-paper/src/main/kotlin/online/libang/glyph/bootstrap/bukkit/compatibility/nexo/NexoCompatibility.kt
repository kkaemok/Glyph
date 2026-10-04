package online.libang.glyph.bootstrap.bukkit.compatibility.nexo

import com.nexomc.nexo.api.events.resourcepack.NexoPrePackGenerateEvent
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
import online.libang.glyph.util.BOOTSTRAP
import online.libang.glyph.util.PLUGIN
import online.libang.glyph.util.handle
import online.libang.glyph.util.info
import online.libang.glyph.util.warn
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

class NexoCompatibility : Compatibility {
    override val website: String = "https://polymart.org/product/6901/nexo/"
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
            fun NexoPrePackGenerateEvent.generate() {
                if (!ConfigManagerImpl.mergeWithExternalResources) return
                when (val result = PLUGIN.reload()) {
                    is ReloadState.Success -> {
                        result.directory()?.let {
                            addResourcePack(it)
                            info("Successfully merged with Nexo.")
                        }
                    }
                    is ReloadState.OnReload -> {
                        warn("Glyph is still on reload!")
                    }
                    is ReloadState.Failure -> {
                        result.throwable.handle("Unable to merge with Nexo.")
                    }
                }
            }
        })
    }
}