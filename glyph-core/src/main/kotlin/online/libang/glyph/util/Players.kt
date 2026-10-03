package online.libang.glyph.util

import online.libang.glyph.api.player.HudPlayer

val HudPlayer.textures
    get() = VOLATILE_CODE.getTextureValue(this)
