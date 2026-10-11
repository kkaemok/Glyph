package online.libang.glyph.renderer

import kr.toxicity.hud.api.player.HudPlayer
import online.libang.glyph.api.state.HudState
import kr.toxicity.hud.api.update.UpdateEvent
import java.util.UUID

internal object RenderFrame {
    // A trigger key identifies an interaction, not its complete render context.
    // PopupUpdateEvent wrappers may share that key but carry different iterators.
    private class SourceKey(val source: Any, val args: List<String>, val event: UpdateEvent, val player: UUID) {
        override fun equals(other: Any?): Boolean = other is SourceKey &&
            source === other.source && event === other.event && args == other.args && player == other.player
        override fun hashCode(): Int =
            31 * (31 * (31 * System.identityHashCode(source) + args.hashCode()) + System.identityHashCode(event)) + player.hashCode()
    }
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
        local.get()?.samples?.sample(SourceKey(source, args, event, player.uuid()), block) ?: block()

    fun state(player: HudPlayer): HudState.Snapshot = local.get()?.states?.getOrPut(player.uuid()) {
        player.hudState.snapshot()
    } ?: player.hudState.snapshot()
}
