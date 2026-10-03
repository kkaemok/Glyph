package online.libang.glyph.bootstrap.bukkit.util

import online.libang.glyph.api.bukkit.BukkitBootstrap
import online.libang.glyph.api.bukkit.trigger.HudBukkitEventTrigger
import online.libang.glyph.api.bukkit.update.BukkitEventUpdateEvent
import online.libang.glyph.api.player.HudPlayer
import online.libang.glyph.api.update.UpdateEvent
import online.libang.glyph.manager.PlayerManagerImpl
import online.libang.glyph.util.BOOTSTRAP
import online.libang.glyph.util.LEGACY_SECTION_SERIALIZER
import online.libang.glyph.util.ifNull
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerEvent
import org.bukkit.plugin.Plugin
import java.util.*
import java.util.function.BiConsumer

val HudPlayer.bukkitPlayer
    get() = handle() as Player

val Player.hudPlayer
    get() = PlayerManagerImpl.getHudPlayer(uniqueId).ifNull { "Unable to find this player: $name" }

/**
 * Serializes this string into a MiniMessage-compatible representation.
 * <p>
 * Names obtained from the server (e.g. custom entity names) may contain legacy section formatting
 * codes, which MiniMessage rejects at parse time. Deserializing through the legacy serializer and
 * re-serializing through MiniMessage converts those codes into MiniMessage tags (and escapes
 * MiniMessage-special characters), so the result can be safely embedded into a text pattern.
 * </p>
 */
fun String.toMiniMessageString() = MiniMessage.miniMessage().serialize(LEGACY_SECTION_SERIALIZER.deserialize(this))

fun Event.call(): Boolean {
    Bukkit.getPluginManager().callEvent(this)
    return if (this is Cancellable) !isCancelled else true
}

fun Event.toUpdateEvent(key: Any = UUID.randomUUID()) = BukkitEventUpdateEvent(this, key)

inline fun <reified T : Event, R : Any> UpdateEvent.unwrap(block: (T) -> R): R {
    val evt = source()
    return if (evt is BukkitEventUpdateEvent) {
        val e = evt.event
        if (e is T) block(e)
        else throw RuntimeException("Unsupported event found: ${e.javaClass.simpleName}")
    } else throw RuntimeException("Unsupported update found: ${javaClass.simpleName}")
}

fun <T : Event> createBukkitTrigger(
    clazz: Class<T>,
    valueMapper: (T) -> UUID? = { if (it is PlayerEvent) it.player.uniqueId else null },
    keyMapper: (T) -> Any = { UUID.randomUUID() }
): HudBukkitEventTrigger<T> {
    return object : HudBukkitEventTrigger<T> {
        override fun getEventClass(): Class<T> = clazz
        override fun getKey(t: T): Any = keyMapper(t)
        override fun registerEvent(eventConsumer: BiConsumer<UUID, UpdateEvent>) {

            Bukkit.getPluginManager().registerEvent(clazz, (BOOTSTRAP as BukkitBootstrap).triggerListener(), EventPriority.MONITOR, { _, e ->
                if (clazz.isAssignableFrom(e.javaClass)) {
                    val cast = clazz.cast(e)
                    valueMapper(cast)?.let { uuid ->
                        val wrapper = BukkitEventUpdateEvent(
                            cast,
                            keyMapper(cast)
                        )
                        eventConsumer.accept(uuid, wrapper)
                    }
                }
            }, BOOTSTRAP as Plugin, true)
        }
    }
}