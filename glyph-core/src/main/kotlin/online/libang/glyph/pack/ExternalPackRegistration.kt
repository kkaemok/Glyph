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
        // CraftEngine may already include build.zip through zips-to-merge. Its
        // inputs must contain one Glyph output, including after plugin restart.
        val ownedInputs = listOfNotNull(primary, textureResolvedZip, registeredOutput).map(::identity).toSet()
        try {
            require(Files.isRegularFile(primary) || Files.isDirectory(primary)) { "Missing Glyph pack: $primary" }
            require(identity(primary) != identity(textureResolvedZip)) { "Glyph output cannot use its private registration-copy path" }
            val sources = external.filterNot { identity(it) in ownedInputs }
            val inspection = PackConflictInspector.inspect(primary, sources, format)
            val output = if (inspection.textureOwners.isEmpty()) primary else {
                val target = textureResolvedZip.toAbsolutePath().normalize()
                require((sources + listOf(primary)).none { identity(it) == identity(target) }) {
                    "Registration copy cannot overwrite an input pack: $target"
                }
                PackConflictInspector.writeRegistrationCopy(inspection, target)
                textureResolvedZip
            }
            unregister(folders, zips, ownedInputs)
            if (Files.isDirectory(output)) { zips.remove(output); folders.add(output) }
            else { folders.remove(output); zips.add(output) }
            registeredOutput = output
            return inspection
        } catch (failure: Exception) {
            unregister(folders, zips, ownedInputs)
            throw IllegalArgumentException("Unable to register Glyph pack: ${failure.message}", failure)
        }
    }

    private fun identity(path: Path): Path = if (Files.exists(path)) path.toRealPath() else path.toAbsolutePath().normalize()

    private fun unregister(folders: MutableSet<Path>, zips: MutableSet<Path>, ownedInputs: Set<Path>) {
        folders.removeIf { identity(it) in ownedInputs }
        zips.removeIf { identity(it) in ownedInputs }
        registeredOutput = null
    }
}
