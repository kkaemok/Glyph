package online.libang.glyph.hud

import online.libang.glyph.api.component.PixelComponent
import online.libang.glyph.api.player.HudPlayer
import online.libang.glyph.api.update.UpdateEvent
import online.libang.glyph.location.PixelLocation
import online.libang.glyph.layout.HeadLayout
import online.libang.glyph.player.head.HeadKey
import online.libang.glyph.player.head.HeadRenderType.*
import online.libang.glyph.renderer.HeadRenderer
import online.libang.glyph.location.GuiLocation
import online.libang.glyph.manager.EncodeManager
import online.libang.glyph.shader.HudShader
import online.libang.glyph.util.*

class HudHeadParser(parent: HudImpl, private val head: HeadLayout, gui: GuiLocation, pixel: PixelLocation) : HudSubParser {

    private val renderer = run {
        val final = head.location + pixel
        val render = head.renderScale + pixel
        val shader = HudShader(
            gui,
            render,
            head.layer,
            head.outline,
            final.opacity,
            head.property
        )
        HeadRenderer(
            head,
            parent.getOrCreateSpace(-1),
            parent.getOrCreateSpace(-(head.source.pixel * 8 + 1)),
            parent.getOrCreateSpace(-(head.source.pixel + 1)),
            (0..7).map { i ->
                val encode = "pixel_${head.source.pixel}".encodeKey(EncodeManager.EncodeNamespace.TEXTURES)
                val fileName = "$NAME_SPACE_ENCODED:$encode.png"
                val ascent = final.y + i * head.source.pixel
                val height = head.source.pixel
                val char = parent.newChar
                val mainChar = head(head.identifier(shader, ascent, fileName)) {
                    parent.jsonArray?.let { array ->
                        createAscent(shader, ascent) { y ->
                            array += jsonObjectOf(
                                "type" to "bitmap",
                                "file" to fileName,
                                "ascent" to y,
                                "height" to height,
                                "chars" to jsonArrayOf(char)
                            )
                        }
                    }
                    char
                }
                when (head.type) {
                    STANDARD -> HeadKey(mainChar, mainChar)
                    FANCY -> {
                        val hair = shader.toFancyHead()
                        HeadKey(
                            mainChar,
                            head(head.identifier(hair, ascent - head.source.pixel, fileName)) {
                                val twoChar = parent.newChar
                                parent.jsonArray?.let { array ->
                                    createAscent(hair, ascent - head.source.pixel) { y ->
                                        array += jsonObjectOf(
                                            "type" to "bitmap",
                                            "file" to fileName,
                                            "ascent" to y,
                                            "height" to height,
                                            "chars" to jsonArrayOf(twoChar)
                                        )
                                    }
                                }
                                twoChar
                            }
                        )
                    }
                }
            },
            parent.imageKey,
            head.source.pixel * 8,
            final.x
        ).render(UpdateEvent.EMPTY)
    }

    override fun render(player: HudPlayer): (Long) -> PixelComponent = renderer(player)
}