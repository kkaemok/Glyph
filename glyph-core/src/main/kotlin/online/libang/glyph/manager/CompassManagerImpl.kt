package online.libang.glyph.manager

import online.libang.glyph.api.compass.Compass
import online.libang.glyph.api.manager.CompassManager
import online.libang.glyph.api.plugin.ReloadInfo
import online.libang.glyph.compass.CompassImpl
import online.libang.glyph.compass.CompassType
import online.libang.glyph.resource.GlobalResource
import online.libang.glyph.util.*
import java.io.File
import java.util.*

object CompassManagerImpl : GlyphManager, CompassManager {

    override val managerName: String = "Background"
    override val supportExternalPacks: Boolean = true

    private val compassMap = HashMap<String, CompassImpl>()

    override fun start() {
    }


    override fun preReload() {
        compassMap.clear()
    }

    override fun reload(workingDirectory: File, info: ReloadInfo, resource: GlobalResource) {
        compassMap.clear()
        val assets = workingDirectory.subFolder("assets")
        workingDirectory.subFolder("compasses").forEachAllYaml(info.sender) { f, s, c ->
            runCatching {
                compassMap.putSync("compass") {
                    c["type"]?.asString().ifNull { "type value not set." }.run {
                        CompassType.valueOf(uppercase()).build(resource, assets, s, c)
                    }
                }
            }.handleFailure(info) {
                "Unable to load this compass: $s in ${f.name}"
            }
        }
    }

    override fun end() {
    }

    override fun getCompass(name: String): Compass? = synchronized(compassMap) {
        compassMap[name]
    }
    override fun getAllNames(): Set<String> = Collections.unmodifiableSet(compassMap.keys)

    override fun getAllCompasses(): Set<Compass> = compassMap.values.toSet()
}