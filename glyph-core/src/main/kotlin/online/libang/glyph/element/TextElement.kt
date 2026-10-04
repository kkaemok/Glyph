package online.libang.glyph.element

import kr.toxicity.hud.api.yaml.YamlObject
import online.libang.glyph.placeholder.ConditionSource
import online.libang.glyph.text.HudTextArray
import online.libang.glyph.text.ImageTextScale
import online.libang.glyph.text.TextScale
import online.libang.glyph.util.IntKeyMap

class TextElement(
    override val id: String,
    val textScale: Int?,
    val array: List<HudTextArray>,
    val charWidth: IntKeyMap<TextScale>,
    val imageTextScale: IntKeyMap<ImageTextScale>,
    yamlObject: YamlObject
) : HudElement, ConditionSource by ConditionSource.Impl(yamlObject)