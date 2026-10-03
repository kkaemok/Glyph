package online.libang.glyph.pack

import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.Collections
import java.util.TreeMap

internal class PackConflictException(path: String, first: String, second: String) :
    IllegalArgumentException("Conflicting resource '$path' from $first and $second; select one owner or merge explicitly. Shader/post-effect composition is not automatic.")

internal class CompiledPack private constructor(val files: Map<String, ByteArray>, val hash: String) {
    class Builder {
        private val files = TreeMap<String, ByteArray>()
        private val origins = HashMap<String, String>()
        fun add(path: String, bytes: ByteArray, origin: String = "Glyph"): Builder {
            validatePath(path)
            val previous = files[path]
            if (previous != null && !previous.contentEquals(bytes)) throw PackConflictException(path, origins.getValue(path), origin)
            if (previous == null) { files[path] = bytes.copyOf(); origins[path] = origin }
            return this
        }
        fun build(): CompiledPack {
            val digest = MessageDigest.getInstance("SHA-256")
            for ((path, bytes) in files) {
                val name = path.toByteArray(Charsets.UTF_8)
                digest.update(ByteBuffer.allocate(4).putInt(name.size).array())
                digest.update(name)
                digest.update(ByteBuffer.allocate(8).putLong(bytes.size.toLong()).array())
                digest.update(bytes)
            }
            return CompiledPack(Collections.unmodifiableMap(TreeMap(files)), java.util.HexFormat.of().formatHex(digest.digest()))
        }
    }
    companion object {
        fun validatePath(path: String) {
            require(path.isNotEmpty() && !path.startsWith('/') && '\\' !in path && ':' !in path && '\u0000' !in path) { "Invalid pack path: $path" }
            require(path.split('/').none { it.isEmpty() || it == "." || it == ".." }) { "Invalid pack path: $path" }
            val asset = Regex("(?:[^/]+/)?assets/([^/]+)/(.*)").matchEntire(path)
            if (asset != null) {
                require(asset.groupValues[1].matches(Regex("[a-z0-9_.-]+"))) { "Invalid resource namespace: $path" }
                require(asset.groupValues[2].matches(Regex("[a-z0-9/._-]+"))) { "Invalid resource identifier: $path" }
            }
        }
        fun of(files: Map<String, ByteArray>): CompiledPack = Builder().apply {
            files.forEach { (path, bytes) -> add(path, bytes) }
        }.build()
    }
}
