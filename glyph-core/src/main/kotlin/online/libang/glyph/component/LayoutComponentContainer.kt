package online.libang.glyph.component

import online.libang.glyph.api.component.PixelComponent
import online.libang.glyph.api.component.WidthComponent
import online.libang.glyph.layout.enums.LayoutAlign
import online.libang.glyph.layout.enums.LayoutOffset
import online.libang.glyph.util.EMPTY_WIDTH_COMPONENT
import online.libang.glyph.util.toSpaceComponent

class LayoutComponentContainer(
    private val offset: LayoutOffset,
    private val align: LayoutAlign,
    private val max: Int
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