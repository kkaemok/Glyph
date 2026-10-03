package online.libang.glyph.bootstrap.bukkit.compatibility

import online.libang.glyph.api.listener.HudListener
import online.libang.glyph.api.placeholder.HudPlaceholder
import online.libang.glyph.api.trigger.HudTrigger
import online.libang.glyph.api.update.UpdateEvent
import online.libang.glyph.api.yaml.YamlObject

interface Compatibility {

    val website: String

    val triggers: Map<String, (YamlObject) -> HudTrigger<*>>
    val listeners: Map<String, (YamlObject) -> (UpdateEvent) -> HudListener>
    val numbers: Map<String, HudPlaceholder<Number>>
    val strings: Map<String, HudPlaceholder<String>>
    val booleans: Map<String, HudPlaceholder<Boolean>>

    fun start() {}
}