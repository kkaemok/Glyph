package online.libang.glyph.pack

import org.jetbrains.annotations.ApiStatus
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipFile

/** Check effective assets before handing a generated pack to another compiler. */
@ApiStatus.Internal
object PackConflictInspector {
    fun validate(primary: Path, external: Collection<Path>, format: Int) {
        validate(primary, external, PackMeta.VersionFormat(format))
    }

    fun validate(primary: Path, external: Collection<Path>, format: PackMeta.VersionFormat) {
        val builder = CompiledPack.Builder()
        for (path in (listOf(primary) + external).distinct()) {
            readAssets(path, format).forEach { (name, bytes) -> builder.add(name, bytes, path.toString()) }
        }
    }

    private fun readAssets(path: Path, format: PackMeta.VersionFormat): Map<String, ByteArray> {
        if (!Files.exists(path)) return emptyMap()
        val raw = HashMap<String, ByteArray>()
        if (Files.isDirectory(path)) {
            val root = path.toRealPath()
            Files.walk(path).use { paths -> paths.filter { Files.isRegularFile(it) }.forEach {
                require(it.toRealPath().startsWith(root)) { "Pack symlink escapes root: $it" }
                val name = path.relativize(it).toString().replace('\\', '/')
                CompiledPack.validatePath(name)
                raw[name] = Files.readAllBytes(it)
            } }
        } else ZipFile(path.toFile()).use { zip ->
            for (entry in zip.entries()) if (!entry.isDirectory) {
                CompiledPack.validatePath(entry.name)
                val bytes = zip.getInputStream(entry).use { it.readAllBytes() }
                val old = raw.putIfAbsent(entry.name, bytes)
                require(old == null || old.contentEquals(bytes)) { "Conflicting duplicate ZIP entry: ${entry.name} in $path" }
            }
        }
        val effective = raw.filterKeys { it.startsWith("assets/") }.toMutableMap()
        raw["pack.mcmeta"]?.let { bytes ->
            for (entry in PackMeta.from(bytes).overlays?.entries.orEmpty()) {
                if (entry.appliesTo(format)) {
                    CompiledPack.validatePath(entry.directory)
                    val prefix = "${entry.directory}/assets/"
                    raw.filterKeys { it.startsWith(prefix) }.forEach { (name, data) ->
                        effective[name.removePrefix("${entry.directory}/")] = data
                    }
                }
            }
        }
        return effective
    }
}
