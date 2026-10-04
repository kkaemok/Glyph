package online.libang.glyph.bootstrap.velocity

import com.velocitypowered.proxy.connection.client.ConnectedPlayer
import com.velocitypowered.proxy.connection.MinecraftConnection
import com.velocitypowered.proxy.protocol.packet.BossBarPacket
import com.velocitypowered.proxy.protocol.packet.chat.ComponentHolder
import io.netty.channel.ChannelDuplexHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelPromise
import online.libang.glyph.api.GlyphAPI
import online.libang.glyph.manager.ConfigManagerImpl
import online.libang.glyph.transport.BossBarStateTracker
import online.libang.glyph.transport.BossBarSlotPlan
import online.libang.glyph.transport.ExternalBossBarText
import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.text.Component
import java.util.UUID

/** Connection-owned state; updates preserve fields omitted from partial Velocity packets. */
internal class VelocityBossBarTransport(private val player: ConnectedPlayer, color: BossBar.Color) {
    private val connection = player.connection
    private val channel = connection.channel
    private val hudId = UUID.randomUUID()
    private var dummyIds = List((ConfigManagerImpl.bossbarLine - 1).coerceAtLeast(0)) { UUID.randomUUID() }
    private var owned = dummyIds.toSet() + hudId
    private val tracker = BossBarStateTracker<State>()
    private val sent = HashMap<UUID, State>()
    private var assignments = emptyMap<UUID, UUID>()
    private var selected: UUID? = null
    private var selectedName: Component? = null
    private var refreshAdditional = false
    private var mergeMode = GlyphAPI.inst().isMergeBossBar
    private var hud = State(color = color)
    private var closed = false
    private var bypass = false
    private val pipelineName = "glyph_bossbar"

    private data class State(val name: Component = Component.empty(), val progress: Float = 0f,
        val color: BossBar.Color = BossBar.Color.WHITE, val overlay: BossBar.Overlay = BossBar.Overlay.PROGRESS,
        val flags: Set<BossBar.Flag> = emptySet()) {
        fun bar() = BossBar.bossBar(name, progress, color, overlay, flags)
    }

    init {
        channel.eventLoop().execute {
            val pipeline = channel.pipeline()
            val key = pipeline.names().firstOrNull { pipeline[it] is MinecraftConnection }
                ?: error("Cannot find Velocity connection handler for Glyph")
            pipeline.addBefore(key, pipelineName, object : ChannelDuplexHandler() {
                override fun write(ctx: ChannelHandlerContext, message: Any, promise: ChannelPromise) {
                    if (message is BossBarPacket && intercept(message)) promise.trySuccess()
                    else super.write(ctx, message, promise)
                }
            })
            dummyIds.forEach { publish(it, State(color = color), true) }
            publish(hudId, hud, true)
        }
    }
    fun update(color: BossBar.Color, component: Component) {
        channel.eventLoop().execute {
            if (closed) return@execute
            syncMergeMode()
            if (refreshAdditional) reconcile()
            val lines = (ConfigManagerImpl.bossbarLine - 1).coerceAtLeast(0)
            if (lines != dummyIds.size) {
                dummyIds.forEach(::remove)
                dummyIds = List(lines) { UUID.randomUUID() }
                owned = dummyIds.toSet() + hudId
                dummyIds.forEach { publish(it, State(color = hud.color), true) }
                reconcile()
            }
            hud = hud.copy(name = component, color = color)
            publish(hudId, selected?.let { tracker[it]?.copy(name = component) } ?: hud,
                forceName = ConfigManagerImpl.forceUpdate)
        }
    }
    fun close() {
        channel.eventLoop().execute {
            if (closed) return@execute
            closed = true
            for ((id, state) in tracker.entries()) if (assignments[id] != id) publish(id, state, true)
            owned.forEach(::remove)
            if (channel.pipeline()[pipelineName] != null) channel.pipeline().remove(pipelineName)
            tracker.clear(); sent.clear()
            GlyphAPI.inst().playerManager.getHudPlayer(player.uniqueId)?.additionalComponent = null
        }
    }
    private fun send(packet: BossBarPacket) {
        bypass = true
        try { connection.write(packet) } finally { bypass = false }
    }
    private fun remove(id: UUID) { send(BossBarPacket.createRemovePacket(id, hud.bar())); sent.remove(id) }
    private fun publish(id: UUID, state: State, add: Boolean = false, forceName: Boolean = false) {
        val previous = sent[id]
        if (!add && !forceName && previous == state) return
        val bar = state.bar()
        if (add || previous == null) send(BossBarPacket.createAddPacket(id, bar, ComponentHolder(connection.protocolVersion, state.name)))
        else {
            if (forceName || previous.name != state.name) send(BossBarPacket.createUpdateNamePacket(id, bar, ComponentHolder(connection.protocolVersion, state.name)))
            if (previous.progress != state.progress) send(BossBarPacket.createUpdateProgressPacket(id, bar))
            if (previous.color != state.color || previous.overlay != state.overlay) send(BossBarPacket.createUpdateStylePacket(id, bar))
            if (previous.flags != state.flags) send(BossBarPacket.createUpdatePropertiesPacket(id, bar))
        }
        sent[id] = state
    }
    private fun flags(packet: BossBarPacket): Set<BossBar.Flag> = buildSet {
        if (packet.flags.toInt() and 1 != 0) add(BossBar.Flag.DARKEN_SCREEN)
        if (packet.flags.toInt() and 2 != 0) add(BossBar.Flag.PLAY_BOSS_MUSIC)
        if (packet.flags.toInt() and 4 != 0) add(BossBar.Flag.CREATE_WORLD_FOG)
    }
    private fun intercept(packet: BossBarPacket): Boolean {
        if (closed || bypass || packet.uuid in owned) return false
        syncMergeMode()
        if (!mergeMode) return false
        val id = packet.uuid
        val current = tracker[id]
        when (packet.action) {
            0 -> tracker.put(id, State(packet.name?.component ?: Component.empty(), packet.percent,
                BossBar.Color.entries[packet.color], BossBar.Overlay.entries[packet.overlay], flags(packet)))
            1 -> {
                if (tracker.remove(id) == null) return false
                if (assignments[id] == id) remove(id)
            }
            2 -> tracker.put(id, (current ?: return false).copy(progress = packet.percent))
            3 -> tracker.put(id, (current ?: return false).copy(name = packet.name?.component ?: Component.empty()))
            4 -> tracker.put(id, (current ?: return false).copy(color = BossBar.Color.entries[packet.color], overlay = BossBar.Overlay.entries[packet.overlay]))
            5 -> tracker.put(id, (current ?: return false).copy(flags = flags(packet)))
            else -> return false
        }
        reconcile()
        return true
    }
    private fun syncMergeMode() {
        if (GlyphAPI.inst().isOnReload) return
        val next = GlyphAPI.inst().isMergeBossBar
        if (next == mergeMode) return
        if (!next) {
            for ((id, state) in tracker.entries()) if (assignments[id] != id) publish(id, state, true)
            tracker.clear()
            reconcile()
        }
        mergeMode = next
    }
    private fun reconcile() {
        val states = tracker.entries()
        val plan = BossBarSlotPlan.allocate(states, dummyIds, hudId) {
            ExternalBossBarText.canMerge(it.name, GlyphAPI.inst().defaultKey)
        }
        val next = plan.assignments
        states.forEach { (id, state) ->
            val slot = next.getValue(id)
            if (assignments[id] == id && slot != id) remove(id)
            publish(slot, state.copy(name = plan.title(id, state.name, hud.name)), slot == id && assignments[id] != id)
        }
        dummyIds.filter { it !in next.values }.forEach { publish(it, State(color = hud.color)) }
        val chosen = plan.merged?.let { id -> states.first { it.first == id } }
        if (!GlyphAPI.inst().isOnReload && (refreshAdditional || chosen?.first != selected || chosen?.second?.name != selectedName)) {
            selected = chosen?.first; selectedName = chosen?.second?.name
            val hudPlayer = GlyphAPI.inst().playerManager.getHudPlayer(player.uniqueId)
            hudPlayer?.additionalComponent = chosen?.second?.name?.let { ExternalBossBarText.measure(it) }
        }
        refreshAdditional = GlyphAPI.inst().isOnReload
        if (chosen == null) publish(hudId, hud)
        assignments = next
    }
}
