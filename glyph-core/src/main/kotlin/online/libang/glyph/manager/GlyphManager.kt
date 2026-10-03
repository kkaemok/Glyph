package online.libang.glyph.manager

import online.libang.glyph.api.plugin.ReloadInfo
import online.libang.glyph.resource.GlobalResource
import java.io.File

interface GlyphManager {

    val managerName: String
    val supportExternalPacks: Boolean

    fun start()
    fun preReload() {}
    fun reload(workingDirectory: File, info: ReloadInfo, resource: GlobalResource)
    fun postReload() {}
    fun end()
}