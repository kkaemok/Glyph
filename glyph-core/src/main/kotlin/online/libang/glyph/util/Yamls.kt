package online.libang.glyph.util

import kr.toxicity.command.BetterCommandSource
import online.libang.glyph.animation.AnimationType
import online.libang.glyph.api.yaml.YamlElement
import online.libang.glyph.api.yaml.YamlObject
import online.libang.glyph.equation.TEquation
import online.libang.glyph.placeholder.ColorOverride
import online.libang.glyph.placeholder.ConditionBuilder
import online.libang.glyph.placeholder.Conditions
import online.libang.glyph.placeholder.PlaceholderSource
import online.libang.glyph.yaml.YamlArrayImpl
import online.libang.glyph.yaml.YamlElementImpl
import online.libang.glyph.yaml.YamlObjectImpl
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.io.InputStream

private val YAML = Yaml()

fun Any.toYaml(path: String): YamlElement = when (this) {
    is Map<* ,*> -> YamlObjectImpl(path, LinkedHashMap<String, Any>().also {
        entries.forEach { e ->
            it[(e.key ?: return@forEach).toString()] = e.value ?: return@forEach
        }
    })
    is List<*> -> YamlArrayImpl(path, this)
    else -> YamlElementImpl(path, this)
}

fun File.toYaml(): YamlObject = synchronized(YAML) {
    inputStream().buffered().use {
        YamlObjectImpl(
            "",
            YAML.load(it) ?: mutableMapOf<String, Any>()
        )
    }
}

fun InputStream.toYaml() = synchronized(YAML) {
    YamlObjectImpl(
        "",
        YAML.load(this) ?: mutableMapOf<String, Any>()
    )
}

fun Map<String, Any>.saveToYaml(file: File) {
    synchronized(YAML) {
        file.bufferedWriter().use {
            it.write(YAML.dumpAsMap(this))
        }
    }
}

fun YamlObject.getAsAnimationType(key: String, sender: BetterCommandSource = BOOTSTRAP.consoleSource()) = get(key)?.asString()?.let {
    runCatching {
        AnimationType.valueOf(it.uppercase())
    }.getOrElse { e ->
        e.handle(sender, "This animation doesn't exist.")
        AnimationType.LOOP
    }
} ?: AnimationType.LOOP


fun File.forEachAllYaml(sender: BetterCommandSource, block: (File, String, YamlObject) -> Unit) {
    forEachAllFolder {
        if (it.extension == "yml") {
            runCatching {
                it.toYaml().forEach { e ->
                    val v = e.value
                    if (v is YamlObject) block(it, e.key, v)
                }
            }.handleFailure(sender) {
                "Unable to load this yml file: ${it.name}"
            }
        } else {
            sender.warn("This is not a yml file: ${it.path}")
        }
    }
}

fun YamlObject.toConditions(source: PlaceholderSource) = get("conditions")?.asObject()?.let {
    Conditions.parse(it, source)
} ?: ConditionBuilder.alwaysTrue
fun YamlObject.toColorOverrides(source: PlaceholderSource) = get("color-overrides")?.asObject()?.let {
    ColorOverride.builder(it, source)
} ?: ColorOverride.empty

fun YamlObject.getTEquation(key: String) = get(key)?.asString()?.let {
    TEquation(it)
}

fun YamlObject.getShadow(key: String) = when (val value = getAsString(key, "0")) {
    "false" -> 0
    "true" -> 0xFF shl 24
    else -> (value.toLong(16) and 0xFFFFFFFF).toInt()
}