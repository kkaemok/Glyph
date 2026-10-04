package online.libang.glyph.util

import kr.toxicity.hud.api.player.HudPlayer

val HudPlayer.textures
    get() = VOLATILE_CODE.getTextureValue(this)
