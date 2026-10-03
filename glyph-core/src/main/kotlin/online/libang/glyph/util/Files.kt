package online.libang.glyph.util

import online.libang.glyph.api.manager.ConfigManager
import online.libang.glyph.api.yaml.YamlObject
import online.libang.glyph.pack.PackMeta
import java.io.File

fun File.subFolder(dir: String) = File(this, dir).apply {
    if (!exists()) mkdir()
}

fun File.subFile(name: String) = File(this, name).apply {
    if (!exists()) createNewFile()
}

fun File.ifNotExist(lazyMessage: () -> String) = takeIf { exists() }.ifNull(lazyMessage)

fun File.forEach(block: (File) -> Unit) {
    listFiles()?.sortedBy {
        it.name
    }?.forEach(block)
}

fun File.isNotEmptyDirectory() = listFiles()?.isNotEmpty() == true

fun File.forEachAllFolder(block: (File) -> Unit) {
    if (name.startsWith('-')) return debug(ConfigManager.DebugLevel.FILE, "File skipped: $path")
    if (isDirectory) forEach {
        it.forEachAllFolder(block)
    } else block(this)
}

fun YamlObject.forEachSubConfiguration(block: (String, YamlObject) -> Unit) {
    forEach {
        val v = it.value
        if (v is YamlObject) block(it.key, v)
    }
}
fun <T> YamlObject.mapSubConfiguration(block: (String, YamlObject) -> T) = mapNotNull {
    val v = it.value
    if (v is YamlObject) block(it.key, v) else null
}

fun File.toMcmeta() = PackMeta.from(this)
fun ByteArray.toMcmeta() = PackMeta.from(this)