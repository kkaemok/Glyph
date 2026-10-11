package online.libang.glyph.shader

/** Remove unused uniform dependencies before the client compiles the shared text shader. */
internal data class ShaderFeatures(val properties: Int, val staticScale: Boolean) {
    fun constants() = mapOf(
        "GLYPH_SHADER_PROPERTIES" to properties.toString(),
        "GLYPH_STATIC_SCALE" to if (staticScale) "1" else "0"
    )

    // API extensions can read Globals or change property at runtime. Keep their
    // existing shader environment rather than guessing from their GLSL source.
    fun withExtensions(hasExtensions: Boolean) = if (hasExtensions) FULL else this

    companion object {
        val NONE = ShaderFeatures(0, false)
        private val FULL = ShaderFeatures(ShaderProperty.entries.fold(0) { mask, property -> mask or property.id }, true)

        fun from(shaders: Collection<HudShader>) = ShaderFeatures(
            shaders.fold(0) { mask, shader -> mask or shader.property },
            shaders.any { it.renderScale.scale.staticScale }
        )
    }
}
