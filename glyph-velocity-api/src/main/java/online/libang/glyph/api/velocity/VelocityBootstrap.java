package online.libang.glyph.api.velocity;

import online.libang.glyph.api.GlyphBootstrap;

/**
 * Represents Velocity bootstrap.
 */
public interface VelocityBootstrap extends GlyphBootstrap {
    @Override
    default boolean isFolia() {
        return false;
    }
    @Override
    default boolean isPaper() {
        return false;
    }
    @Override
    default boolean isVelocity() {
        return true;
    }
}
