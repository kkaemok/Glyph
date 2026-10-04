package online.libang.glyph.api;

import kr.toxicity.hud.api.BetterHudBootstrap;
import kr.toxicity.hud.api.player.HudPlayer;
import online.libang.glyph.api.effect.PostEffects;
import org.jetbrains.annotations.NotNull;

/** Glyph capabilities added to the original BetterHud bootstrap contract. */
public interface GlyphBootstrap extends BetterHudBootstrap {
    @Override @NotNull GlyphLogger logger();
    @Override @NotNull Glyph core();

    /** Optional platform capability, available on Paper 26.3+. */
    default PostEffects postEffects(HudPlayer player) { return PostEffects.unsupported(); }

    /** The resource format's minor version, separate from its major version. */
    default int mcmetaMinorVersion() { return 0; }
}
