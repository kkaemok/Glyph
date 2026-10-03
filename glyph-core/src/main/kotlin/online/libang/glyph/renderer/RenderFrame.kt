package online.libang.glyph.renderer

import online.libang.glyph.api.player.HudPlayer
import online.libang.glyph.api.state.HudState
import online.libang.glyph.api.update.UpdateEvent
import java.util.UUID

internal object RenderFrame {
    private data class SourceKey(val source: Any, val args: List<String>, val event: Any, val player: UUID)
    private class Frame {
        val samples = FrameSamples()
        val states = HashMap<UUID, HudState.Snapshot>()
    }
    private val local = ThreadLocal<Frame>()

    fun <T> render(block: () -> T): T {
        if (local.get() != null) return block()
        local.set(Frame())
        return try { block() } finally { local.remove() }
    }
    fun <T : Any> sample(source: Any, args: List<String>, event: UpdateEvent, player: HudPlayer, block: () -> T): T =
        local.get()?.samples?.sample(SourceKey(source, args, event.key, player.uuid()), block) ?: block()

    fun state(player: HudPlayer): HudState.Snapshot = local.get()?.states?.getOrPut(player.uuid()) {
        player.hudState.snapshot()
    } ?: player.hudState.snapshot()
}
