package online.libang.glyph.bootstrap.bukkit.util

import online.libang.glyph.util.BOOTSTRAP
import org.bukkit.Bukkit
import org.bukkit.event.Listener
import org.bukkit.plugin.Plugin

fun registerListener(listener: Listener) {
    Bukkit.getPluginManager().registerEvents(listener, BOOTSTRAP as Plugin)
}