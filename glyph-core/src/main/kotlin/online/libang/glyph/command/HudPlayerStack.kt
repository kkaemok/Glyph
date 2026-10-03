package online.libang.glyph.command

import online.libang.glyph.api.player.HudPlayer
import java.util.Collections

class HudPlayerStack(
    private val playerList: Collection<HudPlayer>
) : Iterable<HudPlayer> {
    override fun iterator(): Iterator<HudPlayer> = Collections.unmodifiableCollection(playerList).iterator()
}