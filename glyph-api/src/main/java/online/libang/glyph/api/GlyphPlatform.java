package online.libang.glyph.api;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;
import java.util.function.Predicate;

/**
 * Glyph's platform.
 */
@RequiredArgsConstructor
public enum GlyphPlatform {
    /**
     * Checks Paper
     */
    PAPER(GlyphBootstrap::isPaper),
    /**
     * Checks Velocity
     */
    VELOCITY(GlyphBootstrap::isVelocity),
    /**
     * Checks Fabric server
     */
    ;

    /**
     * All platform.
     */
    public static final @NotNull @Unmodifiable Set<GlyphPlatform> ALL = Collections.unmodifiableSet(EnumSet.allOf(GlyphPlatform.class));

    private final @NotNull Predicate<GlyphBootstrap> predicate;

    /**
     * Checks platform matching
     * @param bootstrap Glyph bootstrap
     * @return whether to match or nut
     */
    @ApiStatus.Internal
    public boolean match(@NotNull GlyphBootstrap bootstrap) {
        return predicate.test(bootstrap);
    }
}
