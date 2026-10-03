package online.libang.glyph.api.effect;

import net.kyori.adventure.key.Key;
import java.util.List;

/** Screen effects, separate from glyph widgets. Invoke on the player's owning thread. */
public interface PostEffects {
    boolean supported();
    List<Key> values();
    boolean add(Key key);
    boolean remove(Key key);
    boolean clear();
    boolean set(List<Key> keys);
    static PostEffects unsupported() { return Unsupported.INSTANCE; }
    final class Unsupported {
        private Unsupported() {}
        static final PostEffects INSTANCE = new PostEffects() {
            public boolean supported() { return false; }
            public List<Key> values() { return List.of(); }
            private UnsupportedOperationException error() {
                return new UnsupportedOperationException("Post-effects require Paper 26.3 or newer");
            }
            public boolean add(Key key) { throw error(); }
            public boolean remove(Key key) { throw error(); }
            public boolean clear() { throw error(); }
            public boolean set(List<Key> keys) { throw error(); }
        };
    }
}
