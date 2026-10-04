package online.libang.glyph.transport.paper

import net.minecraft.network.chat.Component
import net.minecraft.network.chat.FontDescription
import net.minecraft.network.chat.Style
import net.minecraft.network.protocol.game.ClientboundBossEventPacket
import net.minecraft.resources.Identifier
import net.minecraft.world.BossEvent
import java.util.UUID
import kotlin.test.*

class BossBarPacketCodecTest {
    private fun state(name: Component): BossEvent = object : BossEvent(UUID.randomUUID(), name,
        BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10) {}.apply {
        progress = .7f
        setDarkenScreen(true)
        setPlayBossMusic(true)
        setCreateWorldFog(true)
    }

    @Test fun `remapping keeps the exact native title and every bossbar property`() {
        val font = FontDescription.Resource(Identifier.parse("customnameplates:default"))
        val shifted = FontDescription.Resource(Identifier.parse("customnameplates:shift_1"))
        val original = Component.literal("\uE001\uF800").withStyle(Style.EMPTY.withFont(font).withBold(true))
            .append(Component.literal("time").withStyle(Style.EMPTY.withFont(shifted).withItalic(true)))
            .append(Component.translatable("example.weather"))
        val source = state(original)
        val slot = UUID.randomUUID()
        val copy = BossBarPacketCodec.copy(slot, source)
        assertEquals(slot, copy.id)
        assertSame(original, copy.name)
        assertEquals(font, copy.name.style.font)
        assertEquals(shifted, copy.name.siblings[0].style.font)
        assertTrue(copy.name.style.isBold)
        assertTrue(copy.name.siblings[0].style.isItalic)
        assertEquals(source.progress, copy.progress)
        assertEquals(source.color, copy.color)
        assertEquals(source.overlay, copy.overlay)
        assertTrue(copy.shouldDarkenScreen())
        assertTrue(copy.shouldPlayBossMusic())
        assertTrue(copy.shouldCreateWorldFog())
    }

    @Test fun `native add name style progress properties and remove packets keep slot and payload`() {
        val source = state(Component.literal("first"))
        var added = false; var renamed = false; var restyled = false
        var progressed = false; var properties = false; var removed = false
        val handler = object : ClientboundBossEventPacket.Handler {
            override fun add(id: UUID, name: Component, progress: Float, color: BossEvent.BossBarColor,
                overlay: BossEvent.BossBarOverlay, darken: Boolean, music: Boolean, fog: Boolean) {
                assertEquals(source.id, id); assertSame(source.name, name)
                assertEquals(.7f, progress); assertEquals(source.color, color); assertEquals(source.overlay, overlay)
                assertTrue(darken && music && fog); added = true
            }
            override fun updateName(id: UUID, name: Component) {
                assertEquals(source.id, id); assertSame(source.name, name); renamed = true
            }
            override fun updateStyle(id: UUID, color: BossEvent.BossBarColor, overlay: BossEvent.BossBarOverlay) {
                assertEquals(source.id, id); assertEquals(source.color, color); assertEquals(source.overlay, overlay)
                restyled = true
            }
            override fun updateProgress(id: UUID, progress: Float) {
                assertEquals(source.id, id); assertEquals(source.progress, progress); progressed = true
            }
            override fun updateProperties(id: UUID, darken: Boolean, music: Boolean, fog: Boolean) {
                assertEquals(source.id, id); assertFalse(darken); assertTrue(music); assertFalse(fog); properties = true
            }
            override fun remove(id: UUID) { assertEquals(source.id, id); removed = true }
        }
        ClientboundBossEventPacket.createAddPacket(source).dispatch(handler)
        source.name = Component.literal("\uE002\uF830").withStyle(Style.EMPTY.withFont(
            FontDescription.Resource(Identifier.parse("other_plugin:offsets"))))
            .append(Component.literal("latest"))
        ClientboundBossEventPacket.createUpdateNamePacket(source).dispatch(handler)
        source.color = BossEvent.BossBarColor.PURPLE; source.overlay = BossEvent.BossBarOverlay.NOTCHED_20
        ClientboundBossEventPacket.createUpdateStylePacket(source).dispatch(handler)
        source.progress = .2f
        ClientboundBossEventPacket.createUpdateProgressPacket(source).dispatch(handler)
        source.setDarkenScreen(false); source.setCreateWorldFog(false)
        ClientboundBossEventPacket.createUpdatePropertiesPacket(source).dispatch(handler)
        ClientboundBossEventPacket.createRemovePacket(source.id).dispatch(handler)
        assertTrue(added && renamed && restyled && progressed && properties && removed)
    }
}
