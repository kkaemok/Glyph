package online.libang.glyph.yaml

import online.libang.glyph.api.yaml.YamlArray
import online.libang.glyph.api.yaml.YamlElement
import online.libang.glyph.api.yaml.YamlObject
import online.libang.glyph.util.ifNull
import java.io.Serializable

class YamlElementImpl(
    path: String,
    private val any: Any
) : YamlConfigurationImpl(path), YamlElement {
    override fun get(): Any = any
    override fun asString(): String = if (any is Serializable) any.toString() else throw RuntimeException("This is not a string: $any")
    override fun asInt(): Int = (any as? Number)?.toInt().ifNull { "This is not a int: $any" }
    override fun asFloat(): Float = (any as? Number)?.toFloat().ifNull { "This is not a float: $any" }
    override fun asDouble(): Double = (any as? Number)?.toDouble().ifNull { "This is not a double: $any" }
    override fun asBoolean(): Boolean = (any as? Boolean).ifNull { "This is not a boolean: $any" }
    override fun asLong(): Long = (any as? Number)?.toLong().ifNull { "This is not a boolean: $any" }
    override fun asArray(): YamlArray = (any as? YamlArray).ifNull { "This is not a yaml array: $any" }
    override fun asObject(): YamlObject = (any as? YamlObject).ifNull { "This is not a yaml object: $any" }
}