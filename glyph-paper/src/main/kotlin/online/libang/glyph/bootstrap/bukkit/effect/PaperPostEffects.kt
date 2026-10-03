package online.libang.glyph.bootstrap.bukkit.effect

import online.libang.glyph.api.effect.PostEffects
import net.kyori.adventure.key.Key
import org.bukkit.entity.Player

/** Only instantiated on Paper 26.3+, keeping older servers free of API linkage. */
class PaperPostEffects(private val player: Player) : PostEffects {
    override fun supported() = true
    override fun values(): List<Key> = player.postEffects().values()
    override fun add(key: Key): Boolean = player.postEffects().add(key)
    override fun remove(key: Key): Boolean = player.postEffects().remove(key)
    override fun clear(): Boolean = player.postEffects().clear()
    override fun set(keys: List<Key>): Boolean = player.postEffects().set(keys)
}
