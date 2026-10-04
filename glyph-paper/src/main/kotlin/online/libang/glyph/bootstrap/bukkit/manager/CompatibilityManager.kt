package online.libang.glyph.bootstrap.bukkit.manager

import kr.toxicity.hud.api.update.UpdateEvent
import online.libang.glyph.bootstrap.bukkit.compatibility.craftengine.CraftEngineCompatibility
import online.libang.glyph.bootstrap.bukkit.compatibility.mmocore.MMOCoreCompatibility
import online.libang.glyph.bootstrap.bukkit.compatibility.mmoitems.MMOItemsCompatibility
import online.libang.glyph.bootstrap.bukkit.compatibility.mythiclib.MythicLibCompatibility
import online.libang.glyph.bootstrap.bukkit.compatibility.mythicmobs.MythicMobsCompatibility
import online.libang.glyph.bootstrap.bukkit.compatibility.nexo.NexoCompatibility
import online.libang.glyph.bootstrap.bukkit.compatibility.parties.PartiesCompatibility
import online.libang.glyph.bootstrap.bukkit.compatibility.skript.SkriptCompatibility
import online.libang.glyph.bootstrap.bukkit.compatibility.vault.VaultCompatibility
import online.libang.glyph.bootstrap.bukkit.compatibility.worldguard.WorldGuardCompatibility
import online.libang.glyph.util.PLUGIN
import online.libang.glyph.util.handleFailure
import org.bukkit.Bukkit
import java.util.function.Function

object CompatibilityManager {

    val compatibilities = mapOf(
        "MMOCore" to {
            MMOCoreCompatibility()
        },
        "MythicMobs" to {
            MythicMobsCompatibility()
        },
        "WorldGuard" to {
            WorldGuardCompatibility()
        },
        "Vault" to {
            VaultCompatibility()
        },
        "MythicLib" to {
            MythicLibCompatibility()
        },
        "Skript" to {
            SkriptCompatibility()
        },
        "MMOItems" to {
            MMOItemsCompatibility()
        },
        "Parties" to {
            PartiesCompatibility()
        },
        "CraftEngine" to {
            CraftEngineCompatibility()
        },
        "Nexo" to {
            NexoCompatibility()
        }
    )

    fun start() {
        compatibilities.forEach {
            if (Bukkit.getPluginManager().isPluginEnabled(it.key)) {
                val obj = it.value()
                runCatching {
                    obj.start()
                    val namespace = it.key.lowercase()
                    obj.listeners.forEach { entry ->
                        PLUGIN.listenerManager.addListener("${namespace}_${entry.key}") { c ->
                            val reason = entry.value(c)
                            Function { u: UpdateEvent ->
                                reason(u)
                            }
                        }
                    }
                    obj.numbers.forEach { entry ->
                        PLUGIN.placeholderManager.numberContainer.addPlaceholder("${namespace}_${entry.key}", entry.value)
                    }
                    obj.strings.forEach { entry ->
                        PLUGIN.placeholderManager.stringContainer.addPlaceholder("${namespace}_${entry.key}", entry.value)
                    }
                    obj.booleans.forEach { entry ->
                        PLUGIN.placeholderManager.booleanContainer.addPlaceholder("${namespace}_${entry.key}", entry.value)
                    }
                    obj.triggers.forEach { entry ->
                        PLUGIN.triggerManager.addTrigger("${namespace}_${entry.key}", entry.value)
                    }
                }.handleFailure {
                    "Unable to load ${it.key} support. checks this: ${obj.website}"
                }
            }
        }
    }
}