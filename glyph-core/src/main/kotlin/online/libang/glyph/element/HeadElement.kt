package online.libang.glyph.element

import kr.toxicity.hud.api.yaml.YamlObject
import online.libang.glyph.placeholder.ConditionSource

class HeadElement(
    override val id: String,
    yaml: YamlObject
) : HudElement, ConditionSource by ConditionSource.Impl(yaml) {
    val pixel = yaml.getAsInt("pixel", 1).coerceAtLeast(1)
}