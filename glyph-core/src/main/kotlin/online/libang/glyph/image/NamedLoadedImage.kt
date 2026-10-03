package online.libang.glyph.image

import online.libang.glyph.manager.EncodeManager
import online.libang.glyph.util.encodeFile

class NamedLoadedImage(
    name: String,
    val image: LoadedImage
) {
    val name = "image_$name".encodeFile(EncodeManager.EncodeNamespace.TEXTURES)
}