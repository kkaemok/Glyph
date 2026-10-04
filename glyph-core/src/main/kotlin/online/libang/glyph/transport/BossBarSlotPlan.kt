package online.libang.glyph.transport

import org.jetbrains.annotations.ApiStatus
import java.util.UUID

/** Same allocation policy on Paper and Velocity. Standalone slots retain native client centering. */
@ApiStatus.Internal
data class BossBarSlotPlan(val assignments: Map<UUID, UUID>, val merged: UUID?) {
    fun <T> title(id: UUID, original: T, hud: T): T = if (id == merged) hud else original

    companion object {
        fun <S> allocate(states: List<Pair<UUID, S>>, dummyIds: List<UUID>, hudId: UUID,
                         canMerge: (S) -> Boolean): BossBarSlotPlan {
            val assignments = LinkedHashMap<UUID, UUID>()
            var merged: UUID? = null
            for ((index, entry) in states.withIndex()) {
                val (id, state) = entry
                assignments[id] = when {
                    index < dummyIds.size -> dummyIds[index]
                    merged == null && canMerge(state) -> hudId.also { merged = id }
                    else -> id
                }
            }
            return BossBarSlotPlan(assignments, merged)
        }
    }
}
