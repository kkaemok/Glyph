package online.libang.glyph.bootstrap.bukkit.module

import online.libang.glyph.bootstrap.bukkit.module.bukkit.BukkitEntityModule
import online.libang.glyph.bootstrap.bukkit.module.bukkit.BukkitItemModule
import online.libang.glyph.bootstrap.bukkit.module.bukkit.BukkitStandardModule

val MODULE_BUKKIT = mapOf(
    "standard" to {
        BukkitStandardModule()
    },
    "entity" to {
        BukkitEntityModule()
    },
    "item" to {
        BukkitItemModule()
    }
)