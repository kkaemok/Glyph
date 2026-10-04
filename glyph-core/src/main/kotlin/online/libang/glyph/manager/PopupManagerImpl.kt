package online.libang.glyph.manager

import kr.toxicity.hud.api.manager.PopupManager
import kr.toxicity.hud.api.plugin.ReloadInfo
import kr.toxicity.hud.api.popup.Popup
import online.libang.glyph.popup.PopupImpl
import online.libang.glyph.resource.GlobalResource
import online.libang.glyph.util.*
import java.io.File
import java.util.*

object PopupManagerImpl : GlyphManager, PopupManager {

    override val managerName: String = "Popup"
    override val supportExternalPacks: Boolean = true

    private val popupMap = HashMap<String, PopupImpl>()
    override fun start() {

    }

    override fun preReload() {
        popupMap.clear()
    }

    override fun reload(workingDirectory: File, info: ReloadInfo, resource: GlobalResource) {
        workingDirectory.subFolder("popups").forEachAllYaml(info.sender) { file, s, yamlObject ->
            runCatching {
                popupMap.putSync("popup") {
                    PopupImpl(s, resource, yamlObject)
                }
            }.handleFailure(info) {
                "Unable to load this popup: $s in ${file.name}"
            }
        }
    }

    override fun postReload() {
        popupMap.values.forEach {
            it.array = null
        }
    }

    override fun getAllNames(): MutableSet<String> = Collections.unmodifiableSet(popupMap.keys)
    override fun getPopup(name: String): Popup? = popupMap[name]
    override fun getAllPopups(): Set<Popup> = popupMap.values.toSet()
    override fun end() {
    }
}