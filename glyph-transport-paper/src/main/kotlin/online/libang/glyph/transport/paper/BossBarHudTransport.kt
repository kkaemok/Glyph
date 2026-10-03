package online.libang.glyph.transport.paper

import io.netty.channel.Channel
import online.libang.glyph.api.GlyphAPI
import online.libang.glyph.manager.ConfigManagerImpl
import online.libang.glyph.transport.BossBarStateTracker
import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.text.Component
import net.minecraft.network.protocol.game.ClientboundBossEventPacket
import net.minecraft.server.network.ServerGamePacketListenerImpl
import net.minecraft.world.BossEvent
import org.bukkit.entity.Player
import java.util.UUID

/** All packet state and pipeline mutations belong to the connection's Netty event loop. */
class BossBarHudTransport(
    private val player: Player,
    private val listener: ServerGamePacketListenerImpl,
    private val channel: Channel,
    initialColor: BossBar.Color,
) {
    private val hudId = UUID.randomUUID()
    private var color = initialColor
    private var hud = BossBarPacketCodec.event(hudId, Component.empty(), color)
    private var dummyIds = List((GlyphAPI.inst().configManager.bossbarLine - 1).coerceAtLeast(0)) { UUID.randomUUID() }
    private var owned = dummyIds.toSet() + hudId
    private val tracker = BossBarStateTracker<BossEvent>()
    private var assignments = emptyMap<UUID, UUID>()
    private val sent = HashMap<UUID, BossEvent>()
    private var additionalId: UUID? = null
    private var additionalName: net.minecraft.network.chat.Component? = null
    private var refreshAdditional = false
    private var mergeMode = GlyphAPI.inst().isMergeBossBar
    private var closed = false
    private val pipelineName = "glyph_bossbar"

    init {
        channel.eventLoop().execute {
            val pipeline = channel.pipeline()
            val connectionHandler = pipeline.names().firstOrNull { pipeline[it] is net.minecraft.network.Connection }
                ?: error("Cannot find Minecraft connection handler for Glyph bossbar transport")
            pipeline.addBefore(connectionHandler, pipelineName, BossBarInterceptor(this))
            dummyIds.forEach { publish(BossBarPacketCodec.event(it, Component.empty(), color), true) }
            publish(hud, true)
        }
    }

    fun update(newColor: BossBar.Color, component: Component) {
        channel.eventLoop().execute {
            if (closed) return@execute
            syncMergeMode()
            if (refreshAdditional) reconcile()
            val lines = (GlyphAPI.inst().configManager.bossbarLine - 1).coerceAtLeast(0)
            if (lines != dummyIds.size) {
                dummyIds.forEach { send(ClientboundBossEventPacket.createRemovePacket(it)); sent.remove(it) }
                dummyIds = List(lines) { UUID.randomUUID() }
                owned = dummyIds.toSet() + hudId
                dummyIds.forEach { publish(BossBarPacketCodec.event(it, Component.empty(), color), true) }
                reconcile()
            }
            color = newColor
            hud.name = BossBarPacketCodec.native(component)
            hud.color = BossBarPacketCodec.color(color)
            val external = additionalId?.let { tracker[it] }
            publish(external?.let { BossBarPacketCodec.copy(hudId, it).apply { name = hud.name } } ?: hud,
                forceName = ConfigManagerImpl.forceUpdate)
        }
    }

    fun close() {
        channel.eventLoop().execute {
            if (closed) return@execute
            closed = true
            // Restore remapped external bars before removing the interceptor.
            for ((id, state) in tracker.entries()) {
                if (assignments[id] != id) send(ClientboundBossEventPacket.createAddPacket(BossBarPacketCodec.copy(id, state)))
            }
            owned.forEach { send(ClientboundBossEventPacket.createRemovePacket(it)) }
            val pipeline = channel.pipeline()
            if (pipeline[pipelineName] != null) pipeline.remove(pipelineName)
            tracker.clear()
            sent.clear()
            GlyphAPI.inst().playerManager.getHudPlayer(player.uniqueId)?.additionalComponent = null
        }
    }

    private var bypass = false
    private fun publish(state: BossEvent, add: Boolean = false, forceName: Boolean = false) {
        val previous = sent[state.id]
        if (add || previous == null) send(ClientboundBossEventPacket.createAddPacket(state))
        else {
            if (forceName || previous.name != state.name) send(ClientboundBossEventPacket.createUpdateNamePacket(state))
            if (previous.progress != state.progress) send(ClientboundBossEventPacket.createUpdateProgressPacket(state))
            if (previous.color != state.color || previous.overlay != state.overlay)
                send(ClientboundBossEventPacket.createUpdateStylePacket(state))
            if (previous.shouldDarkenScreen() != state.shouldDarkenScreen() ||
                previous.shouldPlayBossMusic() != state.shouldPlayBossMusic() ||
                previous.shouldCreateWorldFog() != state.shouldCreateWorldFog())
                send(ClientboundBossEventPacket.createUpdatePropertiesPacket(state))
        }
        sent[state.id] = BossBarPacketCodec.copy(state.id, state)
    }
    private fun send(packet: ClientboundBossEventPacket) {
        bypass = true
        try { listener.send(packet) } finally { bypass = false }
    }

    internal fun intercept(packet: ClientboundBossEventPacket): Boolean {
        if (closed || bypass) return false
        syncMergeMode()
        if (!mergeMode) return false
        var consumed = false
        var changed = false
        packet.dispatch(object : ClientboundBossEventPacket.Handler {
            private fun modify(id: UUID, change: (BossEvent) -> Unit) {
                if (id in owned) return
                val state = tracker[id] ?: return
                change(state); changed = true; consumed = true
            }
            override fun add(id: UUID, name: net.minecraft.network.chat.Component, progress: Float,
                color: BossEvent.BossBarColor, overlay: BossEvent.BossBarOverlay,
                darken: Boolean, music: Boolean, fog: Boolean) {
                if (id in owned) return
                tracker.put(id, object : BossEvent(id, name, color, overlay) {}.apply {
                    this.progress = progress; setDarkenScreen(darken); setPlayBossMusic(music); setCreateWorldFog(fog)
                })
                consumed = true; changed = true
            }
            override fun remove(id: UUID) {
                if (id in owned) return
                if (tracker.remove(id) != null) {
                    if (assignments[id] == id) send(ClientboundBossEventPacket.createRemovePacket(id))
                    sent.remove(id)
                    consumed = true; changed = true
                }
            }
            override fun updateName(id: UUID, name: net.minecraft.network.chat.Component) = modify(id) { it.name = name }
            override fun updateProgress(id: UUID, progress: Float) = modify(id) { it.progress = progress }
            override fun updateStyle(id: UUID, color: BossEvent.BossBarColor, overlay: BossEvent.BossBarOverlay) = modify(id) {
                it.color = color; it.overlay = overlay
            }
            override fun updateProperties(id: UUID, darken: Boolean, music: Boolean, fog: Boolean) = modify(id) {
                it.setDarkenScreen(darken); it.setPlayBossMusic(music); it.setCreateWorldFog(fog)
            }
        })
        if (changed) reconcile()
        return consumed
    }

    private fun syncMergeMode() {
        if (GlyphAPI.inst().isOnReload) return
        val next = GlyphAPI.inst().isMergeBossBar
        if (next == mergeMode) return
        if (!next) {
            for ((id, state) in tracker.entries()) if (assignments[id] != id)
                send(ClientboundBossEventPacket.createAddPacket(BossBarPacketCodec.copy(id, state)))
            tracker.clear()
            reconcile()
        }
        mergeMode = next
    }

    private fun reconcile() {
        val states = tracker.entries()
        val next = LinkedHashMap<UUID, UUID>()
        for ((index, entry) in states.withIndex()) {
            val (id, state) = entry
            val slot = if (index < dummyIds.size) dummyIds[index] else if (index == dummyIds.size) hudId else id
            next[id] = slot
            if (assignments[id] == id && slot != id) {
                send(ClientboundBossEventPacket.createRemovePacket(id)); sent.remove(id)
            }
            val packetState = BossBarPacketCodec.copy(slot, state)
            if (slot == hudId) packetState.name = hud.name
            publish(packetState, slot == id && assignments[id] != id)
        }
        for (slot in dummyIds) if (slot !in next.values) {
            val empty = BossBarPacketCodec.event(slot, Component.empty(), color)
            publish(empty)
        }
        val selected = states.getOrNull(dummyIds.size)
        if (!GlyphAPI.inst().isOnReload && (refreshAdditional || selected?.first != additionalId || selected?.second?.name != additionalName)) {
            additionalId = selected?.first
            additionalName = selected?.second?.name
            val hudPlayer = GlyphAPI.inst().playerManager.getHudPlayer(player.uniqueId)
            hudPlayer?.additionalComponent = selected?.second?.name?.let { online.libang.glyph.transport.ExternalBossBarText.measure(hudPlayer, BossBarPacketCodec.adventure(it)) }
        }
        refreshAdditional = GlyphAPI.inst().isOnReload
        if (selected == null) {
            publish(hud)
        }
        assignments = next
    }

}
