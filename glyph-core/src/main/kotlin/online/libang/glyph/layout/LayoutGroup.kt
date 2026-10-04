package online.libang.glyph.layout

import kr.toxicity.command.BetterCommandSource
import kr.toxicity.hud.api.yaml.YamlObject
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
    val flow = FlowLayout(
        FlowLayout.Mode.valueOf(section.getAsString("layout", "absolute").uppercase()),
        section.getAsInt("gap", 0), section.getAsInt("padding", 0),
        section.getAsInt("min-width", 0), section.getAsInt("max-width", Int.MAX_VALUE),
        FlowLayout.Justify.valueOf(section.getAsString("justify", "start").replace('-', '_').uppercase()))

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

    private val children: List<HudLayout<*>> = run {
        data class Child(val kind: String, val name: String, val yaml: YamlObject)
        val definitions = listOf("images", "texts", "heads").flatMap { kind ->
            section[kind]?.asObject()?.map { Child(kind, it.key, it.value.asObject()) } ?: emptyList()
        }.sortedBy { it.yaml.getAsInt("order", 0) }
        var y = flow.padding
        definitions.mapNotNull { child ->
            runCatching {
                val location = when (flow.mode) {
                    FlowLayout.Mode.ABSOLUTE -> loc
                    FlowLayout.Mode.ROW -> loc + PixelLocation(0, flow.padding, 1.0)
                    FlowLayout.Mode.COLUMN -> loc + PixelLocation(flow.padding, y, 1.0)
                    FlowLayout.Mode.STACK -> loc + PixelLocation(flow.padding, flow.padding, 1.0)
                }
                val layout = when (child.kind) {
                    "images" -> ImageLayout.Impl(child.name, this, child.yaml, location)
                    "texts" -> TextLayout.Impl(child.name, this, child.yaml, location)
                    else -> HeadLayout.Impl(child.name, this, child.yaml, location)
                }
                if (flow.mode == FlowLayout.Mode.COLUMN) {
                    val natural = when (layout) {
                        is ImageLayout -> ((layout.candidates.values + layout.source).maxOf { image ->
                            image.image.maxOfOrNull { it.image.image.height * image.scale * layout.scale } ?: 0.0
                        }).toInt()
                        is TextLayout -> layout.line * layout.lineWidth
                        is HeadLayout -> layout.source.pixel * 8
                        else -> 0
                    }
                    val height = child.yaml.getAsInt("cell-height", natural)
                    require(height >= 0) { "Negative column cell-height: ${child.name}" }
                    y = Math.addExact(y, Math.addExact(height, flow.gap))
                }
                layout
            }.getOrElse {
                it.handle(sender, "Unable to load ${child.kind} child ${child.name} in $id")
                null
            }
        }
    }
    val image = children.filterIsInstance<ImageLayout>()
    val text = children.filterIsInstance<TextLayout>()
    val head = children.filterIsInstance<HeadLayout>()

    fun <T> order(images: List<T>, texts: List<T>, heads: List<T>): List<T> {
        val compiled = (image.zip(images) + text.zip(texts) + head.zip(heads)).toMap()
        return children.map { compiled.getValue(it) }
    }

    val animation = section["animations"]?.asObject()?.let { animations ->
        AnimationLocation(animations)
    } ?: AnimationLocation.zero
}
