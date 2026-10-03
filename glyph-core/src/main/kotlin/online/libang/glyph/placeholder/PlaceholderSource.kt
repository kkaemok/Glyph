package online.libang.glyph.placeholder

import online.libang.glyph.api.yaml.YamlObject
import online.libang.glyph.yaml.YamlObjectImpl

interface PlaceholderSource {
    val placeholderOption: YamlObject
    val stringPlaceholderFormat: YamlObject

    companion object {
        val empty = Impl(
            YamlObjectImpl.empty,
            YamlObjectImpl.empty
        )
    }

    class Impl(
        override val placeholderOption: YamlObject,
        override val stringPlaceholderFormat: YamlObject
    ) : PlaceholderSource {
        constructor(source: YamlObject): this(
            source["placeholder-option"]?.asObject() ?: YamlObjectImpl.empty,
            source["placeholder-string-format"]?.asObject() ?: YamlObjectImpl.empty
        )
    }
}