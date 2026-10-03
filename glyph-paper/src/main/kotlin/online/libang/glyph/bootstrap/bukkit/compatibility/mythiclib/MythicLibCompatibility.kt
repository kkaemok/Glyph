package online.libang.glyph.bootstrap.bukkit.compatibility.mythiclib

import io.lumine.mythic.lib.api.event.PlayerAttackEvent
import online.libang.glyph.api.listener.HudListener
import online.libang.glyph.api.placeholder.HudPlaceholder
import online.libang.glyph.api.trigger.HudTrigger
import online.libang.glyph.api.update.UpdateEvent
import online.libang.glyph.api.yaml.YamlObject
import online.libang.glyph.bootstrap.bukkit.compatibility.Compatibility
import online.libang.glyph.bootstrap.bukkit.util.createBukkitTrigger
import online.libang.glyph.bootstrap.bukkit.util.unwrap
import java.util.function.Function

class MythicLibCompatibility : Compatibility {

    override val website: String = "https://www.spigotmc.org/resources/90306/"

    override val triggers: Map<String, (YamlObject) -> HudTrigger<*>>
        get() = mapOf(
            "damage" to {
                val weaponCritical = it.getAsBoolean("weapon-critical", false)
                val skillCritical = it.getAsBoolean("skill-critical", false)
                createBukkitTrigger(PlayerAttackEvent::class.java, { e ->
                    val data = e.damage
                    if ((weaponCritical && !data.isWeaponCriticalStrike) || (skillCritical && !data.isSkillCriticalStrike)) null
                    else e.attacker.player.uniqueId
                }) { e ->
                    e.entity.uniqueId
                }
            }
        )
    override val listeners: Map<String, (YamlObject) -> (UpdateEvent) -> HudListener>
        get() = mapOf()
    override val numbers: Map<String, HudPlaceholder<Number>>
        get() = mapOf(
            "attack_damage" to HudPlaceholder.of { _, u ->
                u.unwrap { e: PlayerAttackEvent ->
                    Function {
                        e.damage.damage
                    }
                }
            }
        )
    override val strings: Map<String, HudPlaceholder<String>>
        get() = mapOf()
    override val booleans: Map<String, HudPlaceholder<Boolean>>
        get() = mapOf()
}