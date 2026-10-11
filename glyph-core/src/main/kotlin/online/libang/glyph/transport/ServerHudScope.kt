package online.libang.glyph.transport

import org.jetbrains.annotations.ApiStatus

/** Serializes proxy HUD ownership with backend changes and configuration reloads. */
@ApiStatus.Internal
class ServerHudScope<K>(private val enter: (K) -> Unit, private val leave: (K, Boolean) -> Unit) {
    private val active = HashMap<K, String>()

    fun reconcile(player: K, server: String?, allowedServers: Set<String>) {
        reconcile(player, { server }, { allowedServers })
    }

    @Synchronized
    fun reconcile(player: K, currentServer: () -> String?, configuration: () -> Set<String>) {
        // Velocity events are asynchronous. Read the live backend inside the
        // transition lock so a delayed event cannot reactivate an old server.
        val server = currentServer()
        val allowedServers = configuration()
        val desired = server?.takeIf { it in allowedServers || "*" in allowedServers }
        val previous = active[player]
        if (previous == desired) return
        if (previous != null) {
            active.remove(player)
            // Old-backend bars must not be restored after Velocity cleared them.
            leave(player, previous == server)
        }
        if (desired != null) {
            active[player] = desired
            try {
                enter(player)
            } catch (failure: Exception) {
                active.remove(player)
                leave(player, true)
                throw failure
            }
        }
    }

    @Synchronized
    fun disconnect(player: K) {
        if (active.remove(player) != null) leave(player, false)
    }

    @Synchronized
    fun clear(restoreExternal: Boolean) {
        val players = active.keys.toList()
        active.clear()
        players.forEach { leave(it, restoreExternal) }
    }

    @Synchronized
    fun contains(player: K, server: String?): Boolean = server != null && active[player] == server
}
