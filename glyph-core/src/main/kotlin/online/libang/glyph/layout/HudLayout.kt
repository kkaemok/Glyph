package online.libang.glyph.layout

import online.libang.glyph.api.yaml.YamlObject
import online.libang.glyph.element.HudElement
import online.libang.glyph.location.PixelLocation
import online.libang.glyph.placeholder.ConditionSource
import online.libang.glyph.placeholder.PlaceholderSource
import online.libang.glyph.shader.RenderScale
import online.libang.glyph.shader.ShaderProperty
import online.libang.glyph.util.getShadow

interface HudLayout<T : HudElement> : ConditionSource, PlaceholderSource {
    val source: T
    val outline: Int
    val layer: Int
    val property: Int
    val follow: String?
    val location: PixelLocation
    val cancelIfFollowerNotExists: Boolean
    val renderScale: RenderScale
    val tick: Long

    interface Identifier {
        val name: String
    }

    class Impl<T : HudElement>(
        override val source: T,
        group: LayoutGroup,
        originalLoc: PixelLocation,
        yaml: YamlObject
    ) : HudLayout<T>, ConditionSource by source + ConditionSource.Impl(yaml) + group, PlaceholderSource by PlaceholderSource.Impl(yaml) {
        override val outline: Int = yaml.getShadow("outline")
        override val layer: Int = yaml.getAsInt("layer", 0)
        override val property: Int = ShaderProperty.properties(yaml["properties"]?.asArray())
        override val follow: String? = yaml["follow"]?.asString()
        override val location: PixelLocation = PixelLocation(yaml) + originalLoc + PixelLocation.hotBarHeight
        override val cancelIfFollowerNotExists: Boolean = yaml.getAsBoolean("cancel-if-follower-not-exists", true)
        override val renderScale = RenderScale.fromConfig(location, yaml)
        override val tick: Long = yaml.getAsLong("tick", 1)
    }
}