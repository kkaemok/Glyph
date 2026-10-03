package online.libang.glyph.api.bukkit.event;

import online.libang.glyph.api.plugin.ReloadState;
import lombok.Getter;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Plugin reloaded event.
 */
@Getter
public class PluginReloadedEvent extends Event implements GlyphEvent {
    private final ReloadState state;

    /**
     * Plugin reload ended.
     * @param state reload state
     */
    public PluginReloadedEvent(@NotNull ReloadState state) {
        super(true);
        this.state = state;
    }
    /**
     * Gets event handler
     * @return handler
     */
    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLER_LIST;
    }
    /**
     * Gets event handler
     * @return handler
     */
    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }
}
