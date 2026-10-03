package online.libang.glyph.resource

import online.libang.glyph.manager.EncodeManager
import online.libang.glyph.util.encodeKey
import net.kyori.adventure.key.Key

class KeyResource(val namespace: String) {

    private fun create(name: String) = Key.key(namespace, name)

    val spaceKey = create("space".encodeKey(EncodeManager.EncodeNamespace.FONT))
    val defaultKey = create("default".encodeKey(EncodeManager.EncodeNamespace.FONT))
    val splitterKey = create("splitter".encodeKey(EncodeManager.EncodeNamespace.TEXTURES))
}