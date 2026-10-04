package online.libang.glyph.api;

import kr.toxicity.hud.api.BetterHud;
import kr.toxicity.hud.api.player.HudPlayer;
import online.libang.glyph.api.effect.PostEffects;
import org.jetbrains.annotations.NotNull;

/** Glyph extensions to the original BetterHud API, backed by the same live instance. */
public interface Glyph extends BetterHud {
    String DEFAULT_NAMESPACE = "glyph";

    static @NotNull Glyph getInstance() { return GlyphAPI.inst(); }

    @Override @NotNull GlyphBootstrap bootstrap();

    /** Returns the screen-effect capability for this player. */
    default PostEffects postEffects(HudPlayer player) { return bootstrap().postEffects(player); }
}
