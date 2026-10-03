package online.libang.glyph.renderer

import com.sun.management.ThreadMXBean
import online.libang.glyph.api.state.HudState
import online.libang.glyph.api.state.HudValue
import online.libang.glyph.player.HudRenderCache
import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.text.Component
import java.lang.management.ManagementFactory

/** Actual core primitives, without server/player/network overhead. Not a server benchmark. */
object CoreProfile {
    @JvmStatic fun main(args: Array<String>) {
        val bean = ManagementFactory.getThreadMXBean() as ThreadMXBean
        bean.isThreadAllocatedMemoryEnabled = true
        println("players,scenario,frames,ns_per_player_frame,bytes_per_player_frame,rebuilds,send_decisions")
        for (players in listOf(1, 20, 100)) for (changing in listOf(false, true)) {
            val states = List(players) { HudState() }
            val segments = List(players) { DependencyCache<Component>(listOf("health")) }
            val sends = List(players) { HudRenderCache() }
            var rebuilds = 0L
            var packets = 0L
            val renderers = states.map { state -> {
                rebuilds++
                Component.text("Health: ${state.snapshot().get("health")}")
            } }
            fun frame(tick: Int) {
                for (p in 0 until players) {
                    if (changing) states[p].set("health", HudValue.Number((tick % 20).toDouble()))
                    val component = segments[p].get(states[p].snapshot(), render = renderers[p])
                    if (sends[p].shouldUpdate(component, BossBar.Color.WHITE, false)) packets++
                }
            }
            repeat(20_000, ::frame)
            rebuilds = 0; packets = 0
            val thread = Thread.currentThread().threadId()
            val allocated = bean.getThreadAllocatedBytes(thread)
            val started = System.nanoTime()
            val frames = 100_000
            repeat(frames, ::frame)
            val elapsed = System.nanoTime() - started
            val bytes = bean.getThreadAllocatedBytes(thread) - allocated
            val operations = players.toLong() * frames
            println("$players,${if (changing) "changing" else "static"},$frames,${elapsed / operations},${bytes / operations},$rebuilds,$packets")
        }
    }
}
