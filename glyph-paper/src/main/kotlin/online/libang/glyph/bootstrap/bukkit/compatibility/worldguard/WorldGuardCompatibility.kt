package online.libang.glyph.bootstrap.bukkit.compatibility.worldguard

import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldguard.WorldGuard
import online.libang.glyph.api.listener.HudListener
import online.libang.glyph.api.placeholder.HudPlaceholder
import online.libang.glyph.api.player.HudPlayer
import online.libang.glyph.api.trigger.HudTrigger
import online.libang.glyph.api.update.UpdateEvent
import online.libang.glyph.api.yaml.YamlObject
import online.libang.glyph.bootstrap.bukkit.compatibility.Compatibility
import online.libang.glyph.bootstrap.bukkit.util.bukkitPlayer
import java.util.function.Function

class WorldGuardCompatibility : Compatibility {

    override val website: String = "https://modrinth.com/plugin/worldguard"

    override val triggers: Map<String, (YamlObject) -> HudTrigger<*>>
        get() = mapOf()
    override val listeners: Map<String, (YamlObject) -> (UpdateEvent) -> HudListener>
        get() = mapOf()
    override val numbers: Map<String, HudPlaceholder<Number>>
        get() = mapOf()
    override val strings: Map<String, HudPlaceholder<String>>
        get() = mapOf()
    override val booleans: Map<String, HudPlaceholder<Boolean>>
        get() = mapOf(
            "in_region" to HudPlaceholder.builder<Boolean>()
                .requiredArgsLength(1)
                .function { args, _ ->
                    Function { p ->
                        val loc = p.bukkitPlayer.location
                        WorldGuard.getInstance().platform.regionContainer.get(BukkitAdapter.adapt(loc.world))?.getRegion(args[0])?.contains(loc.blockX, loc.blockY, loc.blockZ) ?: false
                    }
                }
                .build()
        )
}