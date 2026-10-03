package online.libang.glyph.layout

/** Horizontal geometry uses measured output widths; vertical geometry is compiled into font ascents. */
data class FlowLayout(val mode: Mode = Mode.ABSOLUTE, val gap: Int = 0, val padding: Int = 0,
    val minWidth: Int = 0, val maxWidth: Int = Int.MAX_VALUE, val justify: Justify = Justify.START) {
    enum class Mode { ABSOLUTE, ROW, COLUMN, STACK }
    enum class Justify { START, CENTER, END, SPACE_BETWEEN }
    init { require(gap >= 0 && padding >= 0 && minWidth >= 0 && maxWidth >= minWidth) { "Invalid flow sizing" } }
    data class Row(val offsets: IntArray, val width: Int)
    fun row(widths: IntArray): Row {
        require(widths.all { it >= 0 })
        val active = widths.count { it > 0 }
        val content = widths.sumOf { it.toLong() } + gap.toLong() * (active - 1).coerceAtLeast(0)
        require(content + padding.toLong() * 2 <= Int.MAX_VALUE) { "Layout width overflows integer geometry" }
        val width = (content.toInt() + padding * 2).coerceIn(minWidth, maxWidth)
        val free = (width - padding * 2 - content.toInt()).coerceAtLeast(0)
        var x = padding + when (justify) { Justify.CENTER -> free / 2; Justify.END -> free; else -> 0 }
        var placed = 0
        return Row(IntArray(widths.size) { index ->
            if (widths[index] == 0) x else {
                if (placed > 0) x += gap + if (justify == Justify.SPACE_BETWEEN && active > 1)
                    free * placed / (active - 1) - free * (placed - 1) / (active - 1) else 0
                placed++
                x.also { x += widths[index] }
            }
        }, width)
    }
}
