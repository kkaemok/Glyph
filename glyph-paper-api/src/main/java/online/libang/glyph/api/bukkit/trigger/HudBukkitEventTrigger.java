package online.libang.glyph.api.bukkit.trigger;

import online.libang.glyph.api.trigger.HudTrigger;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;

/**
 * Represents the wrapped bukkit event.
 * @param <T> event type
 */
public interface HudBukkitEventTrigger<T extends Event> extends HudTrigger<T> {

    /**
     * Returns the type.
     * @return event type
     */
    @NotNull Class<T> getEventClass();

}
