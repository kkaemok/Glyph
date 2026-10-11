package online.libang.glyph.shader

import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest

/** Upgrade only recognized, unmodified stock templates; custom shaders remain user-owned. */
internal object ShaderTemplateStore {
    // Stock 26.3 vertex templates from e05fdd0a and 3909c168, normalized to LF.
    internal val previous263VertexHashes = setOf(
        "995d6480e754f71b7286523f52a88d9e51715285a0e150f90acd2c870f7066bc",
        "78b45e29afc8d8eeb7844f4499d7803ea7208346710c59e12284c05382157202"
    )

    fun read(path: Path, bundled: String, previousHashes: Set<String> = emptySet()): List<String> {
        val existing = if (Files.exists(path)) Files.readString(path) else null
        if (existing == null || fingerprint(existing) in previousHashes) {
            Files.createDirectories(path.parent)
            Files.writeString(path, bundled)
            return bundled.lines()
        }
        return existing.lines()
    }

    private fun fingerprint(source: String): String = MessageDigest.getInstance("SHA-256")
        .digest(source.replace("\r\n", "\n").toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
}
