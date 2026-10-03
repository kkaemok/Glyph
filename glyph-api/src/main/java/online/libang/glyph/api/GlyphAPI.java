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

    private static Glyph main; //Main instance

    /**
     * Gets a main instance of Glyph.
     * @return Glyph
     */
    public static @NotNull Glyph inst() {
        return Objects.requireNonNull(main);
    }

    /**
     * Sets a main instance of Glyph.
     * @param instance instance
     */
    @ApiStatus.Internal
    public static void inst(@NotNull Glyph instance) {
        if (main != null) throw new RuntimeException();
        main = instance;
    }
}
