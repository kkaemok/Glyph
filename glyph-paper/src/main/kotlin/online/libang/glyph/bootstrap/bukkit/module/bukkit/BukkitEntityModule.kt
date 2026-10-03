package online.libang.glyph.bootstrap.bukkit.module.bukkit

import online.libang.glyph.api.bukkit.trigger.HudBukkitEventTrigger
import online.libang.glyph.api.listener.HudListener
import online.libang.glyph.api.placeholder.HudPlaceholder
import online.libang.glyph.api.update.UpdateEvent
import online.libang.glyph.api.yaml.YamlObject
import online.libang.glyph.bootstrap.bukkit.module.BukkitModule
import online.libang.glyph.bootstrap.bukkit.util.*
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.entity.EntityEvent
import org.bukkit.event.entity.PlayerDeathEvent
import java.util.*
import java.util.function.Function

class BukkitEntityModule : BukkitModule {

    override val triggers: Map<String, (YamlObject) -> HudBukkitEventTrigger<*>>
        get() = mapOf(
            "attack" to {
                createBukkitTrigger(EntityDamageByEntityEvent::class.java, {
                    val attacker = it.damager
                    if (attacker is Player) attacker.uniqueId else if (attacker is Projectile) (attacker.shooter as? Player)?.uniqueId else null
                }, {
                    it.entity.uniqueId
                })
            },
            "damage" to {
                createBukkitTrigger(EntityDamageByEntityEvent::class.java, {
                    val victim = it.entity
                    if (victim is Player) victim.uniqueId else null
                }, {
                    it.entity.uniqueId
                })
            },
            "dead" to {
                createBukkitTrigger(PlayerDeathEvent::class.java, {
                    it.entity.uniqueId
                }, {
                    it.entity.killer?.uniqueId ?: UUID.randomUUID()
                })
            },
            "kill" to {
                createBukkitTrigger(EntityDeathEvent::class.java, {
                    it.entity.killer?.uniqueId
                }, {
                    it.entity.uniqueId
                })
            },
        )
    override val listeners: Map<String, (YamlObject) -> (UpdateEvent) -> HudListener>
        get() = mapOf(
            "health" to {
                { event ->
                    event.unwrap ref@ { target: EntityEvent ->
                        val entity = target.entity.adapt as? LivingEntity ?: return@ref HudListener.EMPTY
                        HudListener {
                            entity.health / entity.maximumHealth
                        }
                    }
                }
            }
        )
    override val numbers: Map<String, HudPlaceholder<Number>>
        get() = mapOf(
            "health" to HudPlaceholder.of { _, u ->
                u.unwrap { e: EntityEvent ->
                    val entity = e.entity.adapt as? LivingEntity ?: return@unwrap Function { 0.0 }
                    Function {
                        entity.health
                    }
                }
            },
            "last_damage" to HudPlaceholder.of { _, u ->
                u.unwrap { e: EntityEvent ->
                    val entity = e.entity.adapt as? LivingEntity ?: return@unwrap Function { 0.0 }
                    Function {
                        entity.lastDamage
                    }
                }
            },
            "last_health" to HudPlaceholder.of { _, u ->
                u.unwrap { e: EntityEvent ->
                    val entity = e.entity.adapt as? LivingEntity ?: return@unwrap Function { 0.0 }
                    Function {
                        entity.health + entity.lastDamage
                    }
                }
            },
            "last_health_percentage" to HudPlaceholder.of { _, u ->
                u.unwrap { e: EntityEvent ->
                    val entity = e.entity.adapt as? LivingEntity ?: return@unwrap Function { 0.0 }
                    Function {
                        (entity.health + entity.lastDamage) / entity.maximumHealth
                    }
                }
            },
            "max_health" to HudPlaceholder.of { _, u ->
                u.unwrap { e: EntityEvent ->
                    val entity = e.entity.adapt as? LivingEntity ?: return@unwrap Function { 0.0 }
                    Function {
                        entity.maximumHealth
                    }
                }
            },
            "health_percentage" to HudPlaceholder.of { _, u ->
                u.unwrap { e: EntityEvent ->
                    val entity = e.entity.adapt as? LivingEntity ?: return@unwrap Function { 0.0 }
                    Function get@ {
                        entity.health / entity.maximumHealth
                    }
                }
            },
        )
    override val strings: Map<String, HudPlaceholder<String>>
        get() = mapOf(
            "name" to HudPlaceholder.of { _, u ->
                u.unwrap { e: EntityEvent ->
                    Function {
                        e.entity.name.toMiniMessageString()
                    }
                }
            },
            "type" to HudPlaceholder.of { _, u ->
                u.unwrap { e: EntityEvent ->
                    Function {
                        @Suppress("DEPRECATION")
                        e.entity.type.key.key
                    }
                }
            },
            "custom_name" to HudPlaceholder.of { _, u ->
                u.unwrap { e: EntityEvent ->
                    val entity = e.entity.adapt
                    Function {
                        (entity.customName ?: entity.name).toMiniMessageString()
                    }
                }
            },
        )
    override val booleans: Map<String, HudPlaceholder<Boolean>>
        get() = mapOf(
            "dead" to HudPlaceholder.of { _, u ->
                u.unwrap { e: EntityEvent ->
                    val entity = e.entity.adapt
                    Function {
                        entity.isDead
                    }
                }
            },
            "frozen" to HudPlaceholder.of { _, u ->
                u.unwrap { e: EntityEvent ->
                    val entity = e.entity.adapt
                    Function {
                        entity.isFrozen
                    }
                }
            },
            "burning" to HudPlaceholder.of { _, u ->
                u.unwrap { e: EntityEvent ->
                    val entity = e.entity.adapt
                    Function {
                        entity.fireTicks > 0
                    }
                }
            }
        )

}