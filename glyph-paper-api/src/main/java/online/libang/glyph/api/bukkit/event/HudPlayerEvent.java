package online.libang.glyph.api.bukkit.event;

import online.libang.glyph.api.player.HudPlayer;
import org.jetbrains.annotations.NotNull;

/**
 * Player event
 */
public interface HudPlayerEvent extends GlyphEvent {
    /**
     * Gets called player
     * @return player
     */
    @NotNull HudPlayer player();
}
