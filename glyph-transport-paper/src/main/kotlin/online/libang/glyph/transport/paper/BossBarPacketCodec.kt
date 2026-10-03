package online.libang.glyph.transport.paper

import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import net.minecraft.world.BossEvent
import org.bukkit.craftbukkit.util.CraftChatMessage
import java.util.UUID

/** Conversion remains isolated for measuring Paper's direct bridge against this baseline. */
internal object BossBarPacketCodec {
    fun native(component: Component): net.minecraft.network.chat.Component =
        CraftChatMessage.fromJSON(GsonComponentSerializer.gson().serialize(component))
    fun adventure(component: net.minecraft.network.chat.Component): Component =
        GsonComponentSerializer.gson().deserialize(CraftChatMessage.toJSON(component))
    fun color(color: BossBar.Color): BossEvent.BossBarColor = BossEvent.BossBarColor.valueOf(color.name)
    fun event(id: UUID, name: Component, color: BossBar.Color): BossEvent =
        BossEvent(id, native(name), color(color), BossEvent.BossBarOverlay.PROGRESS).apply { progress = 0f }
    fun copy(id: UUID, source: BossEvent): BossEvent =
        BossEvent(id, source.name, source.color, source.overlay).apply {
            progress = source.progress
            setDarkenScreen(source.shouldDarkenScreen())
            setPlayBossMusic(source.shouldPlayBossMusic())
            setCreateWorldFog(source.shouldCreateWorldFog())
        }
}
