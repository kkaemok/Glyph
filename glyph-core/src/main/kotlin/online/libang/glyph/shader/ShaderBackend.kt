package online.libang.glyph.shader

import kr.toxicity.hud.api.manager.ShaderManager.ShaderType
import online.libang.glyph.util.BOOTSTRAP
import java.nio.file.Files

/** Version selection and template ownership stay outside the renderer/compiler. */
interface ShaderBackend {
    val shaderVersion: Int
    fun lines(type: ShaderType): List<String>
}

object Shader26LegacyBackend : ShaderBackend {
    override val shaderVersion = 3
    override fun lines(type: ShaderType): List<String> = type.lines()
}

object Shader263Backend : ShaderBackend {
    override val shaderVersion = 4
    override fun lines(type: ShaderType): List<String> {
        val path = BOOTSTRAP.dataFolder().toPath().resolve("shaders/26.3/${type.fileName}")
        if (!Files.exists(path)) {
            Files.createDirectories(path.parent)
            BOOTSTRAP.resource("shader/26.3/${type.fileName}")?.use { Files.copy(it, path) }
                ?: error("Missing Glyph 26.3 shader template: ${type.fileName}")
        }
        return Files.readAllLines(path)
    }
}
