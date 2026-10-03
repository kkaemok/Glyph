package online.libang.glyph.layout

import kotlin.test.*

class FlowLayoutTest {
    @Test fun `row respects measured widths and hidden children`() {
        val row = FlowLayout(FlowLayout.Mode.ROW, gap = 3, padding = 2).row(intArrayOf(10, 0, 20))
        assertContentEquals(intArrayOf(2, 12, 15), row.offsets)
        assertEquals(37, row.width)
    }
    @Test fun `justification distributes available width without rounding loss`() {
        val row = FlowLayout(FlowLayout.Mode.ROW, gap = 1, padding = 2, minWidth = 43,
            justify = FlowLayout.Justify.SPACE_BETWEEN).row(intArrayOf(10, 10, 10))
        assertContentEquals(intArrayOf(2, 16, 31), row.offsets)
        assertEquals(43, row.width)
    }
    @Test fun `empty and constrained rows have stable geometry`() {
        assertEquals(4, FlowLayout(FlowLayout.Mode.ROW, padding = 2).row(intArrayOf()).width)
        assertEquals(15, FlowLayout(FlowLayout.Mode.ROW, maxWidth = 15).row(intArrayOf(10, 20)).width)
        assertFailsWith<IllegalArgumentException> { FlowLayout(gap = -1) }
        assertFailsWith<IllegalArgumentException> { FlowLayout().row(intArrayOf(Int.MAX_VALUE, 1)) }
    }
}
