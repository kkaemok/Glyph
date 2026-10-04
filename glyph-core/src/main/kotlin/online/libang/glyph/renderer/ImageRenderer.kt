package online.libang.glyph.renderer

import kr.toxicity.hud.api.component.PixelComponent
import kr.toxicity.hud.api.player.HudPlayer
import kr.toxicity.hud.api.update.UpdateEvent
import online.libang.glyph.image.ImageComponent
import online.libang.glyph.layout.ImageLayout
import online.libang.glyph.manager.PlaceholderManagerImpl
import online.libang.glyph.manager.PlayerManagerImpl
import online.libang.glyph.util.*
import kotlin.math.ceil
import kotlin.math.roundToInt

class ImageRenderer(
    layout: ImageLayout,
    component: ImageComponent,
    candidates: Map<String, ImageComponent> = emptyMap()
) : ImageLayout by layout, HudRenderer {
    private val followHudPlayer = follow?.let {
        PlaceholderManagerImpl.find(it, this).assertString("This placeholder is not a string: $it")
    }
    private val component = component.apply(outline, color)
    private val candidateComponents = candidates.mapValues { it.value.apply(outline, color) }
    private val sourceParts = dynamicSource?.let { pattern ->
        val matcher = Regex("\\[([^]]+)]")
        var offset = 0
        buildList {
            for (match in matcher.findAll(pattern)) {
                add(pattern.substring(offset, match.range.first) to PlaceholderManagerImpl.find(match.groupValues[1], this@ImageRenderer))
                offset = match.range.last + 1
            }
            add(pattern.substring(offset) to null)
        }
    }

    override fun render(event: UpdateEvent): TickProvider<HudPlayer, PixelComponent> {
        val cond = conditions build event
        val listens = HashMap<ImageComponent, kr.toxicity.hud.api.listener.HudListener>()
        fun register(c: ImageComponent) {
            if (listens.containsKey(c)) return
            listens[c] = c.listener(event)
            c.children.values.forEach(::register)
        }
        (candidateComponents.values + component).forEach(::register)
        val follow = followHudPlayer?.build(event)

        val stackGetter = stack?.build(event)
        val maxStackGetter = maxStack?.build(event)

        val mapper = component mapper event
        val candidateMappers = candidateComponents.mapValues { it.value mapper event }
        val lookup = online.libang.glyph.image.DynamicAssetLookup(candidateMappers, mapper) {
            warn("Missing dynamic image '$it'; using '${source.id}'. Further distinct warnings are capped at 64 per layout.")
        }
        val parts = sourceParts?.map { (literal, value) -> literal to value?.build(event) }
        val colorApply = colorOverrides(event)

        return tickProvide(tick) build@ { player, frame ->
            val selected = if (parts == null) mapper(player) else lookup.select(buildString {
                for ((literal, value) in parts) { append(literal); if (value != null) append(value.value(player)) }
            })(player)
            val listen = listens[selected] ?: selected.listener(event)

            val stackFrame = (stackGetter?.value(player) as? Number)?.toDouble() ?: 0.0
            val maxStackFrame = (maxStackGetter?.value(player) as? Number)?.toInt()?.coerceAtLeast(1) ?: ceil(stackFrame).toInt()

            var target = player
            follow?.let {
                PlayerManagerImpl.getHudPlayer(it.value(player).toString())?.let { p ->
                    target = p
                } ?: run {
                    if (cancelIfFollowerNotExists) return@build EMPTY_PIXEL_COMPONENT
                }
            }
            if (cond(target)) {
                if (maxStackFrame > 1) {
                    if (stackFrame <= 0.0) return@build EMPTY_PIXEL_COMPONENT
                    var empty = EMPTY_PIXEL_COMPONENT
                    val range = 0..<maxStackFrame
                    for (i in if (reversed) range.reversed() else range) {
                        val frame = ((stackFrame - i - 0.1) * selected.images.size)
                            .roundToInt()
                            .coerceAtLeast(0)
                            .coerceAtMost(selected.images.lastIndex)
                        empty = empty.append(space, selected.images[frame])
                    }
                    empty.applyColor(colorApply(target))
                } else selected.type.getComponent(listen, frame, selected, target).applyColor(colorApply(target))
            } else {
                if (clearListener) listen.clear(player)
                EMPTY_PIXEL_COMPONENT
            }
        }
    }

    fun max() = maxOf(component.max, candidateComponents.values.maxOfOrNull { it.max } ?: 0)
}
