package online.libang.glyph.api;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Start of Glyph API.
 */
public final class GlyphAPI {

    //Nn initializer
    private GlyphAPI() {
        throw new RuntimeException();
    }

    /**
     * Gets a main instance of Glyph.
     * @return Glyph
     */
    public static @NotNull Glyph inst() {
        return (Glyph) kr.toxicity.hud.api.BetterHudAPI.inst();
    }

    /**
     * Sets a main instance of Glyph.
     * @param instance instance
     */
    @ApiStatus.Internal
    public static void inst(@NotNull Glyph instance) {
        kr.toxicity.hud.api.BetterHudAPI.inst(Objects.requireNonNull(instance));
    }
}
