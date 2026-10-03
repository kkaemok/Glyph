package online.libang.glyph.bootstrap.velocity.util

import com.velocitypowered.api.proxy.Player
import online.libang.glyph.api.player.HudPlayer

val HudPlayer.velocityPlayer
    get() = handle() as Player