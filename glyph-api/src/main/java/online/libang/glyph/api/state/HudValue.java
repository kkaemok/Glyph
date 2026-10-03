package online.libang.glyph.api.state;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import java.util.Objects;

/** Immutable native values. Text is literal; Component preserves formatting safely. */
public sealed interface HudValue {
    record Number(double value) implements HudValue {
        public Number { if (!Double.isFinite(value)) throw new IllegalArgumentException("HUD numbers must be finite"); }
    }
    record Boolean(boolean value) implements HudValue {}
    record Text(String value) implements HudValue {
        public Text { Objects.requireNonNull(value); }
    }
    record RichText(Component value) implements HudValue {
        public RichText { Objects.requireNonNull(value); }
    }
    record Identifier(Key value) implements HudValue {
        public Identifier { Objects.requireNonNull(value); }
    }
}
