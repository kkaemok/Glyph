package online.libang.glyph.placeholder

import online.libang.glyph.api.player.HudPlayer

interface Placeholder<T : Any> : (HudPlayer) -> T {
    val clazz: Class<out T>
    fun value(player: HudPlayer): T = invoke(player)

    fun stringValue(player: HudPlayer): String
}