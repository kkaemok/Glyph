package online.libang.glyph.image

import java.awt.image.BufferedImage

/** Pack-time nearest-neighbour sampling: fixed corners, stretch or tile edges/centre. */
object NineSlice {
    data class Insets(val left: Int, val top: Int, val right: Int, val bottom: Int)
    enum class Mode { STRETCH, TILE }
    fun resize(source: BufferedImage, width: Int, height: Int, border: Insets,
        mode: Mode = Mode.STRETCH): BufferedImage {
        val (left, top, right, bottom) = border
        require(listOf(left, top, right, bottom).all { it >= 0 }) { "Nine-slice borders must be non-negative" }
        require(source.width > left + right && source.height > top + bottom) { "Nine-slice source must have a centre" }
        require(width >= left + right && height >= top + bottom && width > 0 && height > 0) {
            "Nine-slice target must fit its fixed corners"
        }
        require(width.toLong() * height <= 16_777_216) { "Nine-slice target exceeds 16M pixels" }
        fun coordinate(pos: Int, size: Int, original: Int, start: Int, end: Int): Int = when {
            pos < start -> pos
            pos >= size - end -> original - (size - pos)
            mode == Mode.TILE -> start + (pos - start) % (original - start - end)
            else -> start + ((pos - start).toLong() * (original - start - end) / (size - start - end)).toInt()
        }
        val xs = IntArray(width) { coordinate(it, width, source.width, left, right) }
        val ys = IntArray(height) { coordinate(it, height, source.height, top, bottom) }
        return BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB).apply {
            for (y in 0 until height) for (x in 0 until width) setRGB(x, y, source.getRGB(xs[x], ys[y]))
        }
    }
}
