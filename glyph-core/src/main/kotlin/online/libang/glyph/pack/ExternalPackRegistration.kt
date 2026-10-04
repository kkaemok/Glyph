package online.libang.glyph.pack

import org.jetbrains.annotations.ApiStatus
import java.nio.file.Files
import java.nio.file.Path

/** Validates and registers through the same mutable sets exposed by CraftEngine's cache event. */
@ApiStatus.Internal
class ExternalPackRegistration(private val textureResolvedZip: Path) {
    var registeredOutput: Path? = null
        private set

    fun register(primary: Path, external: Collection<Path>, folders: MutableSet<Path>, zips: MutableSet<Path>,
                 format: PackMeta.VersionFormat): PackConflictInspector.Inspection {
        try {
            require(Files.isRegularFile(primary) || Files.isDirectory(primary)) { "Missing Glyph pack: $primary" }
            val sources = external.filter { it.toAbsolutePath().normalize() != registeredOutput?.toAbsolutePath()?.normalize() &&
                it.toAbsolutePath().normalize() != primary.toAbsolutePath().normalize() }
            val inspection = PackConflictInspector.inspect(primary, sources, format)
            val output = if (inspection.textureOwners.isEmpty()) primary else {
                val target = textureResolvedZip.toAbsolutePath().normalize()
                require((sources + primary).none { it.toAbsolutePath().normalize() == target }) {
                    "Registration copy cannot overwrite an input pack: $target"
                }
                PackConflictInspector.writeRegistrationCopy(inspection, target)
                textureResolvedZip
            }
            unregister(folders, zips)
            if (Files.isDirectory(output)) { zips.remove(output); folders.add(output) }
            else { folders.remove(output); zips.add(output) }
            registeredOutput = output
            return inspection
        } catch (failure: Exception) {
            unregister(folders, zips)
            throw IllegalArgumentException("Unable to register Glyph pack: ${failure.message}", failure)
        }
    }

    private fun unregister(folders: MutableSet<Path>, zips: MutableSet<Path>) {
        registeredOutput?.let { folders.remove(it); zips.remove(it) }
        registeredOutput = null
    }
}
