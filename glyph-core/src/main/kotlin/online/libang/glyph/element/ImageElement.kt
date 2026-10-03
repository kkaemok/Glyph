package online.libang.glyph.element

import online.libang.glyph.api.yaml.YamlArray
import online.libang.glyph.api.yaml.YamlElement
import online.libang.glyph.api.yaml.YamlObject
import online.libang.glyph.image.NamedLoadedImage
import online.libang.glyph.image.enums.ImageType
import online.libang.glyph.manager.ImageManager
import online.libang.glyph.manager.ListenerManagerImpl
import online.libang.glyph.manager.PlaceholderManagerImpl
import online.libang.glyph.placeholder.Conditions
import online.libang.glyph.placeholder.ConditionSource
import online.libang.glyph.placeholder.PlaceholderSource
import online.libang.glyph.util.getAsAnimationType
import online.libang.glyph.util.ifNull

class ImageElement(
    override val id: String,
    val image: List<NamedLoadedImage>,
    val type: ImageType,
    setting: YamlObject
) : HudElement, ConditionSource by ConditionSource.Impl(setting), PlaceholderSource by PlaceholderSource.Impl(setting) {
    val listener = setting["listener"]?.asObject()?.let {
        ListenerManagerImpl.getListener(it)
    }
    val scale = setting.getAsDouble("scale", 1.0).apply {
        if (this <= 0.0) throw RuntimeException("scale cannot be <= 0.0: $id")
    }
    val animationType = setting.getAsAnimationType("animation-type")

    private val childrenMap = when (val child = setting["children"]) {
        is YamlArray -> child.associate {
            it.asString().let { s -> s to s }
        }
        is YamlObject -> child.associate {
            it.key to it.value.asString()
        }
        is YamlElement -> child.asString().let {
            mapOf(it to it)
        }
        null -> emptyMap()
    }

    fun contains(key: String): Boolean = children.containsKey(key) || children.values.any {
        it.contains(key)
    }

    val children by lazy {
        fun String.toImage() = ImageManager.getImage(this).ifNull { "This children image doesn't exist in $id: $this" }
        when {
            childrenMap.isEmpty() -> emptyMap()
            childrenMap.size == 1 -> if (childrenMap.values.first() == "*") ImageManager.allImage.filter {
                it.id != id && !it.contains(id)
            }.associateBy {
                it.id
            } else childrenMap.entries.first().run {
                mapOf(key to value.toImage())
            }
            else -> childrenMap.entries.associate {
                it.key to it.value.toImage()
            }
        }
    }

    val follow = setting["follow"]?.asString()?.let {
        PlaceholderManagerImpl.find(it, this)
            .assertString("This placeholder is not a string in image $id: $it")
    }
    val childrenMapper = setting["children-mapper"]?.asObject()?.map {
        it.key to Conditions.parse(it.value.asObject(), this)
    }
}