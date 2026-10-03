package online.libang.glyph.transport.paper

import io.netty.channel.Channel
import online.libang.glyph.api.GlyphAPI
import online.libang.glyph.api.component.WidthComponent
import online.libang.glyph.api.player.HudPlayer
import online.libang.glyph.transport.BossBarStateTracker
import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.TranslatableComponent
import net.kyori.adventure.text.format.TextDecoration
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
    private val dummyIds = List((GlyphAPI.inst().configManager.bossbarLine - 1).coerceAtLeast(0)) { UUID.randomUUID() }
    private val owned = dummyIds.toSet() + hudId
    private val tracker = BossBarStateTracker<BossEvent>()
    private var assignments = emptyMap<UUID, UUID>()
    private var additionalId: UUID? = null
    private var additionalName: net.minecraft.network.chat.Component? = null
    private var closed = false
    private val pipelineName = "glyph_bossbar"

    init {
        channel.eventLoop().execute {
            val pipeline = channel.pipeline()
            val connectionHandler = pipeline.names().firstOrNull { pipeline[it] is net.minecraft.network.Connection }
                ?: error("Cannot find Minecraft connection handler for Glyph bossbar transport")
            pipeline.addBefore(connectionHandler, pipelineName, BossBarInterceptor(this))
            dummyIds.forEach { send(ClientboundBossEventPacket.createAddPacket(BossBarPacketCodec.event(it, Component.empty(), color))) }
            send(ClientboundBossEventPacket.createAddPacket(hud))
        }
    }

    fun update(newColor: BossBar.Color, component: Component) {
        channel.eventLoop().execute {
            if (closed) return@execute
            val changedColor = color != newColor
            color = newColor
            hud.name = BossBarPacketCodec.native(component)
            hud.color = BossBarPacketCodec.color(color)
            send(ClientboundBossEventPacket.createUpdateNamePacket(hud))
            if (changedColor && additionalId == null) send(ClientboundBossEventPacket.createUpdateStylePacket(hud))
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
            GlyphAPI.inst().playerManager.getHudPlayer(player.uniqueId)?.additionalComponent = null
        }
    }

    private var bypass = false
    private fun send(packet: ClientboundBossEventPacket) {
        bypass = true
        try { listener.send(packet) } finally { bypass = false }
    }

    internal fun intercept(packet: ClientboundBossEventPacket): Boolean {
        if (closed || bypass || !GlyphAPI.inst().isMergeBossBar || GlyphAPI.inst().isOnReload) return false
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
                tracker.put(id, BossEvent(id, name, color, overlay).apply {
                    this.progress = progress; setDarkenScreen(darken); setPlayBossMusic(music); setCreateWorldFog(fog)
                })
                consumed = true; changed = true
            }
            override fun remove(id: UUID) {
                if (id in owned) return
                if (tracker.remove(id) != null) {
                    if (assignments[id] == id) send(ClientboundBossEventPacket.createRemovePacket(id))
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

    private fun reconcile() {
        val states = tracker.entries()
        val next = LinkedHashMap<UUID, UUID>()
        for ((index, entry) in states.withIndex()) {
            val (id, state) = entry
            val slot = if (index < dummyIds.size) dummyIds[index] else if (index == dummyIds.size) hudId else id
            next[id] = slot
            if (assignments[id] == id && slot != id) send(ClientboundBossEventPacket.createRemovePacket(id))
            val packetState = BossBarPacketCodec.copy(slot, state)
            if (slot == id && assignments[id] != id) send(ClientboundBossEventPacket.createAddPacket(packetState))
            else {
                if (slot != hudId) send(ClientboundBossEventPacket.createUpdateNamePacket(packetState))
                send(ClientboundBossEventPacket.createUpdateProgressPacket(packetState))
                send(ClientboundBossEventPacket.createUpdateStylePacket(packetState))
                send(ClientboundBossEventPacket.createUpdatePropertiesPacket(packetState))
            }
        }
        for (slot in dummyIds) if (slot !in next.values) {
            val empty = BossBarPacketCodec.event(slot, Component.empty(), color)
            send(ClientboundBossEventPacket.createUpdateNamePacket(empty))
            send(ClientboundBossEventPacket.createUpdateProgressPacket(empty))
            send(ClientboundBossEventPacket.createUpdateStylePacket(empty))
            send(ClientboundBossEventPacket.createUpdatePropertiesPacket(empty))
        }
        val selected = states.getOrNull(dummyIds.size)
        if (selected?.first != additionalId || selected?.second?.name != additionalName) {
            additionalId = selected?.first
            additionalName = selected?.second?.name
            val hudPlayer = GlyphAPI.inst().playerManager.getHudPlayer(player.uniqueId)
            hudPlayer?.additionalComponent = selected?.second?.name?.let { measure(hudPlayer, BossBarPacketCodec.adventure(it)) }
        }
        if (selected == null) {
            send(ClientboundBossEventPacket.createUpdateNamePacket(hud))
            send(ClientboundBossEventPacket.createUpdateProgressPacket(hud))
            send(ClientboundBossEventPacket.createUpdateStylePacket(hud))
            send(ClientboundBossEventPacket.createUpdatePropertiesPacket(hud))
        }
        assignments = next
    }

    private fun measure(player: HudPlayer, component: Component): WidthComponent {
        val api = GlyphAPI.inst()
        fun font(c: Component): Component = c.font(api.defaultKey).children(c.children().map(::font))
        fun decorated(parent: Boolean, state: TextDecoration.State) = when (state) {
            TextDecoration.State.TRUE -> true; TextDecoration.State.FALSE -> false; TextDecoration.State.NOT_SET -> parent
        }
        fun width(c: Component, bold: Boolean, italic: Boolean): Int {
            val text = when (c) {
                is TextComponent -> c.content()
                is TranslatableComponent -> api.translate(player.locale(), c.key())
                else -> ""
            }
            var total = 0
            var offset = 0
            while (offset < text.length) {
                val point = text.codePointAt(offset); offset += Character.charCount(point)
                total += (if (point == 32) 4 else api.getWidth(point) + 1) + (if (bold) 1 else 0) + (if (italic) 1 else 0)
            }
            for (child in c.children()) total += width(child,
                decorated(bold, child.decoration(TextDecoration.BOLD)), decorated(italic, child.decoration(TextDecoration.ITALIC)))
            return total
        }
        return WidthComponent(Component.text().append(font(component)), width(component,
            decorated(false, component.decoration(TextDecoration.BOLD)), decorated(false, component.decoration(TextDecoration.ITALIC))))
    }
}
