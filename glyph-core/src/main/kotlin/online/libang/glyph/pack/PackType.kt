package online.libang.glyph.pack

import online.libang.glyph.manager.ConfigManagerImpl
import online.libang.glyph.util.DATA_FOLDER
import online.libang.glyph.util.info
import java.io.File
import java.security.MessageDigest

enum class PackType {
    FOLDER, ZIP, NONE;
    fun generate(byteMap: Map<String, ByteArray>): File? {
        if (this == NONE) { PackUploader.stop(); return null }
        val pack = CompiledPack.of(byteMap)
        require(!ConfigManagerImpl.enableProtection) {
            "enable-protection is unsupported: invalid ZIP metadata is incompatible with deterministic packs. Disable it."
        }
        val target = File(DATA_FOLDER.parentFile, ConfigManagerImpl.buildFolderLocation + if (this == ZIP) ".zip" else "")
        when (this) {
            FOLDER -> { PackUploader.stop(); DirectoryPackOutput(ConfigManagerImpl.clearBuildFolder).write(pack, target.toPath()) }
            ZIP -> {
                ZipPackOutput.write(pack, target.toPath())
                val digest = MessageDigest.getInstance("SHA-1")
                target.inputStream().buffered().use { input ->
                    val buffer = ByteArray(8192)
                    while (true) { val count = input.read(buffer); if (count < 0) break; digest.update(buffer, 0, count) }
                }
                val identity = PackUUID.from(digest).apply { save() }
                if (ConfigManagerImpl.enableSelfHost) PackUploader.upload(identity, target.readBytes())
            }
            NONE -> Unit
        }
        info("Compiled $this pack: ${pack.files.size} assets, SHA-256 ${pack.hash}")
        return target
    }
}
