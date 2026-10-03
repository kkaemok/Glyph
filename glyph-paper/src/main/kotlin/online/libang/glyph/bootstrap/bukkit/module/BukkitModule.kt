package online.libang.glyph.bootstrap.bukkit.module

import online.libang.glyph.api.bukkit.trigger.HudBukkitEventTrigger
import online.libang.glyph.api.yaml.YamlObject

interface BukkitModule: Module {
    override val triggers: Map<String, (YamlObject) -> HudBukkitEventTrigger<*>>
}