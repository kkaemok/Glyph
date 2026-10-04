package online.libang.glyph.component

import kr.toxicity.hud.api.component.PixelComponent
import kr.toxicity.hud.api.component.WidthComponent
import online.libang.glyph.layout.enums.LayoutAlign
import online.libang.glyph.layout.enums.LayoutOffset
import online.libang.glyph.util.EMPTY_WIDTH_COMPONENT
import online.libang.glyph.util.toSpaceComponent
import online.libang.glyph.layout.FlowLayout

class LayoutComponentContainer(
    private val offset: LayoutOffset,
    private val align: LayoutAlign,
    private var max: Int,
    private val flow: FlowLayout = FlowLayout()
) {
    private var comp = EMPTY_WIDTH_COMPONENT

    private fun append(other: PixelComponent) {
        val move = when (align) {
            LayoutAlign.LEFT -> 0
            LayoutAlign.CENTER -> (max - other.component.width) / 2
            LayoutAlign.RIGHT -> max - other.component.width
        }
        comp += (other.pixel + move).toSpaceComponent() + other.component + (-other.pixel - other.component.width - move).toSpaceComponent()
    }

    fun append(others: List<PixelComponent>): LayoutComponentContainer {
        if (flow.mode == FlowLayout.Mode.ROW) {
            val row = flow.row(IntArray(others.size) { others[it].component.width.coerceAtLeast(0) })
            max = row.width
            others.forEachIndexed { index, other ->
                val x = other.pixel + row.offsets[index]
                comp += x.toSpaceComponent() + other.component + (-x - other.component.width).toSpaceComponent()
            }
            return this
        }
        others.forEach {
            append(it)
        }
        return this
    }

    fun build(): WidthComponent {
        return when (offset) {
            LayoutOffset.LEFT -> 0
            LayoutOffset.CENTER -> -max / 2
            LayoutOffset.RIGHT -> -max
        }.toSpaceComponent() + comp
    }
}
