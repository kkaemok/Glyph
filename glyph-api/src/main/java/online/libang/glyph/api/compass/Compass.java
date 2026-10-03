package online.libang.glyph.api.compass;

import online.libang.glyph.api.configuration.HudComponentSupplier;
import online.libang.glyph.api.configuration.HudObject;
import online.libang.glyph.api.player.HudPlayer;
import org.jetbrains.annotations.NotNull;

/**
 * Represents compass.
 */
public interface Compass extends HudObject {
    /**
     * Indicates some player's compass by some location
     * @see online.libang.glyph.api.player.PointedLocationProvider
     * @param player target player
     * @return component
     */
    @NotNull
    HudComponentSupplier<Compass> indicate(@NotNull HudPlayer player);
}
