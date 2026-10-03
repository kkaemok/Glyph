package online.libang.glyph.pack

import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.nio.file.Path
import java.nio.file.StandardCopyOption.*
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

internal fun interface PackOutput {
    fun write(pack: CompiledPack, target: Path)
}

internal object ZipPackOutput : PackOutput {
    fun bytes(pack: CompiledPack): ByteArray = ByteArrayOutputStream().use { output ->
        ZipOutputStream(output).use { zip ->
            zip.setLevel(Deflater.BEST_COMPRESSION)
            for ((name, data) in pack.files) {
                zip.putNextEntry(ZipEntry(name).apply { time = 0 })
                zip.write(data)
                zip.closeEntry()
            }
        }
        output.toByteArray()
    }
    override fun write(pack: CompiledPack, target: Path) {
        val output = bytes(pack)
        if (Files.isRegularFile(target, NOFOLLOW_LINKS) && Files.readAllBytes(target).contentEquals(output)) return
        atomicWrite(target, output)
    }
}

internal class DirectoryPackOutput(private val clear: Boolean = true) : PackOutput {
    override fun write(pack: CompiledPack, target: Path) {
        val root = target.toAbsolutePath().normalize()
        require(!Files.isSymbolicLink(root)) { "Pack root must not be a symbolic link: $root" }
        Files.createDirectories(root)
        for ((path, bytes) in pack.files) {
            val file = root.resolve(path).normalize()
            require(file.startsWith(root)) { "Pack path escapes output: $path" }
            var parent: Path? = file
            while (parent != null && parent.startsWith(root)) {
                require(!Files.isSymbolicLink(parent)) { "Pack output contains a symbolic link: $parent" }
                parent = parent.parent
            }
            if (!Files.isRegularFile(file, NOFOLLOW_LINKS) || !Files.readAllBytes(file).contentEquals(bytes)) atomicWrite(file, bytes)
        }
        if (clear) Files.walk(root).use { paths ->
            paths.sorted(Comparator.reverseOrder()).forEach { file ->
                if (file == root) return@forEach
                val relative = root.relativize(file).toString().replace('\\', '/')
                if (Files.isDirectory(file, NOFOLLOW_LINKS)) {
                    Files.list(file).use { if (it.findAny().isEmpty) Files.delete(file) }
                } else if (relative !in pack.files) Files.delete(file)
            }
        }
    }
}

private fun atomicWrite(target: Path, bytes: ByteArray) {
    val absolute = target.toAbsolutePath()
    Files.createDirectories(absolute.parent)
    val temp = Files.createTempFile(absolute.parent, ".glyph-", ".tmp")
    try {
        Files.write(temp, bytes)
        try { Files.move(temp, absolute, ATOMIC_MOVE, REPLACE_EXISTING) }
        catch (_: java.nio.file.AtomicMoveNotSupportedException) { Files.move(temp, absolute, REPLACE_EXISTING) }
    } finally { Files.deleteIfExists(temp) }
}
