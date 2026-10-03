package online.libang.glyph.layout

import kr.toxicity.command.BetterCommandSource
import online.libang.glyph.api.yaml.YamlObject
import online.libang.glyph.configuration.HudConfiguration
import online.libang.glyph.layout.enums.LayoutAlign
import online.libang.glyph.layout.enums.LayoutOffset
import online.libang.glyph.animation.AnimationLocation
import online.libang.glyph.location.PixelLocation
import online.libang.glyph.placeholder.ConditionSource
import online.libang.glyph.util.*

class LayoutGroup(
    override val id: String,
    sender: BetterCommandSource,
    section: YamlObject
) : HudConfiguration, ConditionSource by ConditionSource.Impl(section) {

    private val loc = PixelLocation(section)
    // Opt-in declaration: these layouts promise all changing inputs live in native state.
    val dependencies: List<String>? = section["dependencies"]?.asArray()?.map { it.asString() }

    val align = section["align"]?.asString()?.let {
        runCatching {
            LayoutAlign.valueOf(it.uppercase())
        }.onFailure {
            it.handle(sender, "Unable to find that align: $it")
        }.getOrNull()
    } ?: LayoutAlign.LEFT
    val offset = section["offset"]?.asString()?.let {
        runCatching {
            LayoutOffset.valueOf(it.uppercase())
        }.onFailure {
            it.handle(sender, "Unable to find that offset: $it")
        }.getOrNull()
    } ?: LayoutOffset.CENTER

    val image = section["images"]?.asObject()?.mapSubConfiguration { s, yamlObject ->
        ImageLayout.Impl(s, this, yamlObject, loc)
    } ?: emptyList()
    val text = section["texts"]?.asObject()?.mapSubConfiguration { s, yamlObject ->
        TextLayout.Impl(s, this, yamlObject, loc)
    } ?: emptyList()
    val head = section["heads"]?.asObject()?.mapSubConfiguration { s, yamlObject ->
        HeadLayout.Impl(s, this, yamlObject, loc)
    } ?: emptyList()

    val animation = section["animations"]?.asObject()?.let { animations ->
        AnimationLocation(animations)
    } ?: AnimationLocation.zero
}
