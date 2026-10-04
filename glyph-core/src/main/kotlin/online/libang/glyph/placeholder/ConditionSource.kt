package online.libang.glyph.placeholder

import kr.toxicity.hud.api.yaml.YamlObject
import online.libang.glyph.util.toColorOverrides
import online.libang.glyph.util.toConditions

interface ConditionSource {
    val conditions: ConditionBuilder
    val colorOverrides: ColorOverride.Builder

    operator fun plus(other: ConditionSource) = Impl(
        colorOverrides + other.colorOverrides,
        conditions and other.conditions
    )

    class Impl(
        override val colorOverrides: ColorOverride.Builder,
        override val conditions: ConditionBuilder,
    ) : ConditionSource {
        constructor(yamlObject: YamlObject, source: PlaceholderSource): this(
            yamlObject.toColorOverrides(source),
            yamlObject.toConditions(source),
        )
        constructor(yamlObject: YamlObject): this(
            yamlObject,
            PlaceholderSource.Impl(yamlObject)
        )
    }
}