package online.libang.glyph.manager

import online.libang.glyph.api.manager.TriggerManager
import online.libang.glyph.api.plugin.ReloadInfo
import online.libang.glyph.api.trigger.HudTrigger
import online.libang.glyph.api.yaml.YamlObject
import online.libang.glyph.resource.GlobalResource
import online.libang.glyph.util.ifNull
import java.io.File
import java.util.*
import java.util.function.Function

object TriggerManagerImpl : GlyphManager, TriggerManager {

    override val managerName: String = "Trigger"
    override val supportExternalPacks: Boolean = false

    private val map = mutableMapOf<String, (YamlObject) -> HudTrigger<*>>()

    override fun start() {

    }

    override fun addTrigger(name: String, trigger: Function<YamlObject, HudTrigger<*>>) {
        map[name] = {
            trigger.apply(it)
        }
    }

    fun getTrigger(yamlObject: YamlObject): HudTrigger<*> {
        val clazz = yamlObject["class"]?.asString()?.ifNull { "class value not found." }
        val builder = map[clazz].ifNull { "this class doesn't exist: $clazz" }
        return builder(yamlObject)
    }

    override fun getAllTriggerKeys(): Set<String> = Collections.unmodifiableSet(map.keys)

    override fun reload(workingDirectory: File, info: ReloadInfo, resource: GlobalResource) {
    }

    override fun end() {
    }
}