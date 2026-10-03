package online.libang.glyph.util

import online.libang.glyph.api.component.WidthComponent
import online.libang.glyph.api.configuration.HudObject
import online.libang.glyph.api.configuration.HudObjectType
import online.libang.glyph.layout.HudLayout
import online.libang.glyph.manager.ImageManager
import online.libang.glyph.manager.PlayerHeadManager
import online.libang.glyph.manager.ShaderManagerImpl
import online.libang.glyph.manager.TextManagerImpl
import online.libang.glyph.shader.HudShader
import online.libang.glyph.text.BackgroundKey


const val HUD_DEFAULT_BIT = 13
const val HUD_MAX_BIT = 23 - HUD_DEFAULT_BIT
const val HUD_ADD_HEIGHT = (1 shl HUD_DEFAULT_BIT - 1) - 1

fun createAscent(shader: HudShader, y: Int, consumer: (Int) -> Unit) {
    ShaderManagerImpl.addHudShader(shader) { id ->
        consumer(-((id + (1 shl HUD_MAX_BIT) shl HUD_DEFAULT_BIT) + HUD_ADD_HEIGHT + y))
    }
}

fun text(group: HudLayout.Identifier, creator: () -> BackgroundKey) = TextManagerImpl.getKey(group) ?: creator().apply {
    TextManagerImpl.setKey(group, this)
}
fun image(group: HudLayout.Identifier, creator: () -> WidthComponent) = ImageManager.getImage(group) ?: creator().apply {
    ImageManager.setImage(group, this)
}
fun head(group: HudLayout.Identifier, creator: () -> String) = PlayerHeadManager.getHead(group) ?: creator().apply {
    PlayerHeadManager.setHead(group, this)
}

fun <T : HudObject> Collection<String>.toNonDefaultName(mapper: HudObjectType<T>) = asSequence()
    .mapNotNull(mapper::byName)
    .filter { !it.isDefault }
    .toList()

fun Collection<HudObject>.toNonDefaultNames() = asSequence()
    .filter { !it.isDefault }
    .map { it.name }
    .toList()
fun Collection<String>.toNonDefaultHud() = toNonDefaultName(HudObjectType.HUD)
fun Collection<String>.toNonDefaultPopup() = toNonDefaultName(HudObjectType.POPUP)
fun Collection<String>.toNonDefaultCompass() =toNonDefaultName(HudObjectType.COMPASS)