package online.libang.glyph.shader

import online.libang.glyph.layout.HudLayout

data class ShaderGroup(
    val shader: HudShader,
    override val name: String,
    val ascent: Int
) : HudLayout.Identifier