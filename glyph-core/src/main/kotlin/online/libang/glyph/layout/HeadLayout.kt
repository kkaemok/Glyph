package online.libang.glyph.layout

import kr.toxicity.hud.api.yaml.YamlObject
import online.libang.glyph.layout.enums.LayoutAlign
import online.libang.glyph.location.PixelLocation
import online.libang.glyph.manager.PlayerHeadManager
import online.libang.glyph.player.head.HeadRenderType
import online.libang.glyph.element.HeadElement
import online.libang.glyph.shader.HudShader
import online.libang.glyph.shader.ShaderGroup
import online.libang.glyph.util.ifNull
import online.libang.glyph.util.toLayoutAlign

interface HeadLayout : HudLayout<HeadElement> {
    val type: HeadRenderType
    val align: LayoutAlign

    fun identifier(shader: HudShader, ascent: Int, fileName: String): HudLayout.Identifier {
        return ShaderGroup(shader, fileName, ascent)
    }

    class Impl(
        override val source: HeadElement,
        group: LayoutGroup,
        yamlObject: YamlObject,
        loc: PixelLocation
    ) : HeadLayout, HudLayout<HeadElement> by HudLayout.Impl(source, group, loc, yamlObject) {
        constructor(
            s: String,
            group: LayoutGroup,
            yamlObject: YamlObject,
            loc: PixelLocation
        ): this(
            yamlObject["name"]?.asString().ifNull { "name value not set: $s" }.let {
                PlayerHeadManager.getHead(it).ifNull { "this head doesn't exist: $it in $s" }
            },
            group,
            yamlObject,
            loc
        )
        override val type = HeadRenderType.valueOf(yamlObject.getAsString("type", "standard").uppercase())
        override val align: LayoutAlign = when (type) {
            HeadRenderType.STANDARD -> yamlObject["align"]?.asString().toLayoutAlign()
            HeadRenderType.FANCY -> LayoutAlign.CENTER
        }
    }
}