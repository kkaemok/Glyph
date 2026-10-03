package online.libang.glyph.util

import online.libang.glyph.manager.ConfigManagerImpl
import online.libang.glyph.manager.EncodeManager

fun String.encodeFile(namespace: EncodeManager.EncodeNamespace): String {
    val split = split('.')
    if (split.size != 2) throw RuntimeException("Invalid file name: $this")
    return "${split[0].encodeKey(namespace)}.${split[1]}"
}

fun String.encodeKey(namespace: EncodeManager.EncodeNamespace) = if (ConfigManagerImpl.resourcePackObfuscation) EncodeManager.generateKey(namespace, this) else this