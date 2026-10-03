package online.libang.glyph.api.hud;

import online.libang.glyph.api.configuration.HudComponentSupplier;
import online.libang.glyph.api.configuration.HudObject;
import online.libang.glyph.api.player.HudPlayer;
import org.jetbrains.annotations.NotNull;

/**
 * Represents hud.
 */
public interface Hud extends HudObject {

    /**
     * Returns the output of hud.
     * @param player target player
     * @return component of hud
     */
    @NotNull HudComponentSupplier<Hud> createRenderer(@NotNull HudPlayer player);
}
