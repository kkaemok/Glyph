package online.libang.glyph.bootstrap.bukkit.effect;

import online.libang.glyph.api.effect.PostEffects;
import net.kyori.adventure.key.Key;
import org.bukkit.entity.Player;
import java.util.List;

/** Loaded only on Paper 26.3+. Calls require the player's owning thread. */
public final class PaperPostEffects implements PostEffects {
    private final Player player;
    public PaperPostEffects(Player player) { this.player = player; }
    public boolean supported() { return true; }
    public List<Key> values() { return player.postEffects().values(); }
    public boolean add(Key key) { return player.postEffects().add(key); }
    public boolean remove(Key key) { return player.postEffects().remove(key); }
    public boolean clear() { return player.postEffects().clear(); }
    public boolean set(List<Key> keys) { return player.postEffects().set(keys); }
}
