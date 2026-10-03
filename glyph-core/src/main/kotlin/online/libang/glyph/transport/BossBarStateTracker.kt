package online.libang.glyph.transport

import org.jetbrains.annotations.ApiStatus
import java.util.UUID

/** Owned by one connection's event loop. Ordered state survives promotion between slots. */
@ApiStatus.Internal
class BossBarStateTracker<S> {
    private val states = LinkedHashMap<UUID, S>()
    fun put(id: UUID, state: S) { states[id] = state }
    operator fun get(id: UUID): S? = states[id]
    fun remove(id: UUID): S? = states.remove(id)
    fun entries(): List<Pair<UUID, S>> = states.entries.map { it.key to it.value }
    fun clear() { states.clear() }
}
