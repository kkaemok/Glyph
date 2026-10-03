package online.libang.glyph.bootstrap.velocity.module.velocity

import online.libang.glyph.api.listener.HudListener
import online.libang.glyph.api.placeholder.HudPlaceholder
import online.libang.glyph.api.trigger.HudTrigger
import online.libang.glyph.api.update.UpdateEvent
import online.libang.glyph.api.yaml.YamlObject
import online.libang.glyph.bootstrap.velocity.module.Module
import online.libang.glyph.bootstrap.velocity.util.velocityPlayer
import java.util.function.Function

class VelocityStandardModule : Module {
    override val triggers: Map<String, (YamlObject) -> HudTrigger<*>>
        get() = mapOf(
        )
    override val listeners: Map<String, (YamlObject) -> (UpdateEvent) -> HudListener>
        get() = mapOf(
        )
    override val numbers: Map<String, HudPlaceholder<Number>>
        get() = mapOf(
            "ping" to HudPlaceholder.of { _, _ ->
                Function {
                    it.velocityPlayer.ping
                }
            }
        )
    override val strings: Map<String, HudPlaceholder<String>>
        get() = mapOf(
            "name" to HudPlaceholder.of { _, _ ->
                Function {
                    it.velocityPlayer.username
                }
            }
        )
    override val booleans: Map<String, HudPlaceholder<Boolean>>
        get() = mapOf(
        )
}