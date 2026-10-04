package online.libang.glyph.manager

import kr.toxicity.hud.api.plugin.ReloadInfo
import online.libang.glyph.layout.LayoutGroup
import online.libang.glyph.resource.GlobalResource
import online.libang.glyph.util.*
import java.io.File

object LayoutManager : GlyphManager {

    override val managerName: String = "Layout"
    override val supportExternalPacks: Boolean = true

    private val layoutMap = HashMap<String, LayoutGroup>()

    override fun start() {

    }

    fun getLayout(name: String) = synchronized(layoutMap) {
        layoutMap[name]
    }

    override fun preReload() {
        layoutMap.clear()
    }

    override fun reload(workingDirectory: File, info: ReloadInfo, resource: GlobalResource) {
        workingDirectory.subFolder("layouts").forEachAllYaml(info.sender) { file, s, yamlObject ->
            runCatching {
                layoutMap.putSync("layout") {
                    LayoutGroup(s, info.sender, yamlObject)
                }
            }.handleFailure(info) {
                "Unable to load this layout: $s in ${file.name}"
            }
        }
    }

    override fun end() {
    }
}