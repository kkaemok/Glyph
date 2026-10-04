package online.libang.glyph.pack

import org.jetbrains.annotations.ApiStatus
import com.google.gson.JsonParser
import com.google.gson.JsonElement
import java.io.ByteArrayInputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipFile
import javax.imageio.ImageIO

/** Check effective assets before handing a generated pack to another compiler. */
@ApiStatus.Internal
object PackConflictInspector {
    data class TextureOwner(val resource: String, val owner: Path)
    class Inspection internal constructor(
        val textureOwners: List<TextureOwner>,
        val equivalentTextures: Set<String>,
        internal val primaryFiles: Map<String, ByteArray>
    )

    fun validate(primary: Path, external: Collection<Path>, format: Int) {
        validate(primary, external, PackMeta.VersionFormat(format))
    }

    fun validate(primary: Path, external: Collection<Path>, format: PackMeta.VersionFormat) {
        inspect(primary, external, format)
    }

    /** Validation does not mutate packs. Registration must apply textureOwners to its Glyph copy. */
    fun inspect(primary: Path, external: Collection<Path>, format: PackMeta.VersionFormat): Inspection {
        val packs = (listOf(primary) + external).map { it.toAbsolutePath().normalize() }.distinct()
            .map { readPack(it, format) }
        val protectedTextures = packs.flatMap { protectedTextures(it.files) }.toSet()
        val overlayOwners = HashMap<String, Pair<Pair<PackMeta.VersionFormat, PackMeta.VersionFormat>, Path>>()
        for (pack in packs) for (entry in pack.overlays) {
            val bounds = bounds(entry)
            val previous = overlayOwners.putIfAbsent(entry.directory, bounds to pack.path)
            require(previous == null || previous.first == bounds) {
                "Incompatible overlay '${entry.directory}' in ${previous?.second} and ${pack.path}: different format bounds"
            }
        }
        val owners = HashMap<String, Pair<Source, ByteArray>>()
        val textureOwners = LinkedHashMap<String, TextureOwner>()
        val equivalent = sortedSetOf<String>()
        for (pack in packs) for ((name, bytes) in pack.effective) {
            val previous = owners[name]
            if (previous == null) { owners[name] = pack to bytes; continue }
            if (previous.second.contentEquals(bytes)) { owners[name] = pack to bytes; continue }
            // Font/effect texture dependencies stay strict regardless of their directory.
            val texture = if (name.endsWith(".png.mcmeta")) name.removeSuffix(".mcmeta") else name
            if (!ordinaryTexture(texture, protectedTextures)) throw PackConflictException(name, previous.first.path.toString(), pack.path.toString())
            val sameMetadata = previous.first.effective["$texture.mcmeta"].sameBytes(pack.effective["$texture.mcmeta"])
            if (name == texture && sameMetadata && samePixels(previous.second, bytes)) {
                equivalent += name
                owners[name] = pack to bytes
                continue
            }
            // There must be one unambiguous external owner. Do not silently choose between plugins.
            require(previous.first === packs.first()) {
                "Multiple external texture owners for '$name': ${previous.first.path} and ${pack.path}; choose one owner explicitly"
            }
            // Dropping Glyph's copies across overlays is safe only when the owner supplies a base asset.
            // An overlay-only owner would make the resource disappear on other supported versions.
            require(texture in pack.files) {
                "Incompatible texture overlay for '$name' in ${pack.path}: external owner has no base asset"
            }
            textureOwners[texture] = TextureOwner(texture, pack.path)
            owners[name] = pack to bytes
        }
        return Inspection(textureOwners.values.toList(), equivalent, packs.first().files)
    }

    /** The user's original Glyph/CNP files remain untouched; only this registration copy is filtered. */
    internal fun writeRegistrationCopy(inspection: Inspection, target: Path) {
        val excluded = inspection.textureOwners.flatMap { listOf(it.resource, "${it.resource}.mcmeta") }.toSet()
        val files = inspection.primaryFiles.filterKeys { name ->
            val asset = if (name.startsWith("assets/")) name else name.substringAfter("/assets/", "").let {
                if (it.isEmpty()) "" else "assets/$it"
            }
            asset !in excluded
        }
        ZipPackOutput.write(CompiledPack.of(files), target)
    }

    private fun ordinaryTexture(name: String, protectedTextures: Set<String>): Boolean =
        name.matches(Regex("assets/[^/]+/textures/.+\\.png")) &&
            "/textures/font/" !in name && name !in protectedTextures

    private fun protectedTextures(files: Map<String, ByteArray>): List<String> = buildList {
        fun texture(identifier: String, appendExtension: Boolean = false) {
            val namespace = identifier.substringBefore(':', "minecraft")
            val path = identifier.substringAfter(':', identifier)
            // Font bitmap IDs are relative to textures/, while effect inputs may use full paths.
            val location = if (appendExtension) path.removePrefix("textures/") else path
            add("assets/$namespace/textures/$location" + if (appendExtension && !location.endsWith(".png")) ".png" else "")
        }
        fun effectInputs(element: JsonElement) {
            when {
                element.isJsonArray -> element.asJsonArray.forEach(::effectInputs)
                element.isJsonObject -> element.asJsonObject.entrySet().forEach { (key, value) ->
                    if (key in setOf("texture", "location") && value.isJsonPrimitive && value.asJsonPrimitive.isString)
                        texture(value.asString, true)
                    else effectInputs(value)
                }
            }
        }
        for ((name, bytes) in files) {
            val font = name.matches(Regex("(?:[^/]+/)?assets/[^/]+/font/.+\\.json"))
            val effect = name.matches(Regex("(?:[^/]+/)?assets/[^/]+/(?:shaders|post_effect)/.+\\.json"))
            if (!font && !effect) continue
            // Only discover dependencies here. Definition collisions remain byte-strict.
            val json = runCatching { JsonParser.parseString(bytes.toString(Charsets.UTF_8)).asJsonObject }.getOrNull() ?: continue
            if (effect) effectInputs(json)
            val providers = json.getAsJsonArray("providers") ?: continue
            for (provider in providers) {
                val entry = provider.asJsonObject
                if (entry.get("type")?.asString?.substringAfter(':') != "bitmap") continue
                val file = entry.get("file")?.asString ?: continue
                texture(file)
            }
        }
    }

    private fun ByteArray?.sameBytes(other: ByteArray?): Boolean =
        if (this == null || other == null) this == null && other == null else contentEquals(other)

    private fun samePixels(first: ByteArray, second: ByteArray): Boolean {
        val a = ByteArrayInputStream(first).use { ImageIO.read(it) }
        val b = ByteArrayInputStream(second).use { ImageIO.read(it) }
        require(a != null && b != null) { "Invalid PNG in texture collision" }
        if (a.width != b.width || a.height != b.height) return false
        // Fully transparent RGB is invisible. Glyph's Graphics2D clears/normalizes it differently
        // from CNP's setRGB loop, and PNG encoders may also produce different bytes for equal pixels.
        for (y in 0 until a.height) for (x in 0 until a.width) {
            val left = a.getRGB(x, y); val right = b.getRGB(x, y)
            if (left != right && (left ushr 24 != 0 || right ushr 24 != 0)) return false
        }
        return true
    }

    private data class Source(val path: Path, val files: Map<String, ByteArray>,
                              val effective: Map<String, ByteArray>, val overlays: List<PackMeta.OverlayEntry>)

    private fun bounds(entry: PackMeta.OverlayEntry): Pair<PackMeta.VersionFormat, PackMeta.VersionFormat> =
        (entry.minFormat ?: PackMeta.VersionFormat(entry.formats.min)) to
            (entry.maxFormat ?: PackMeta.VersionFormat(entry.formats.max, Int.MAX_VALUE))

    private fun readPack(path: Path, format: PackMeta.VersionFormat): Source {
        if (!Files.exists(path)) return Source(path, emptyMap(), emptyMap(), emptyList())
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
        val overlays = raw["pack.mcmeta"]?.let { bytes ->
            try { PackMeta.from(bytes).overlays?.entries.orEmpty() }
            catch (failure: RuntimeException) { throw IllegalArgumentException("Invalid pack.mcmeta in $path", failure) }
        }.orEmpty()
        require(overlays.map { it.directory }.distinct().size == overlays.size) { "Duplicate overlay directory in $path" }
        for (entry in overlays) {
            require(entry.directory.matches(Regex("[a-zA-Z0-9_.-]+")) && entry.directory != "." && entry.directory != "..") {
                "Invalid overlay directory '${entry.directory}' in $path"
            }
            val (lower, upper) = bounds(entry)
            require(lower <= upper) { "Inverted overlay bounds for '${entry.directory}' in $path" }
            if (entry.appliesTo(format)) {
                CompiledPack.validatePath(entry.directory)
                val prefix = "${entry.directory}/assets/"
                raw.filterKeys { it.startsWith(prefix) }.forEach { (name, data) ->
                    effective[name.removePrefix("${entry.directory}/")] = data
                }
            }
        }
        return Source(path, raw, effective, overlays)
    }
}
