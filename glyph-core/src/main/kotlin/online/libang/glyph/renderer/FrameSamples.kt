package online.libang.glyph.renderer

/** A frame samples source values once before applying formatting/casts per consumer. */
internal class FrameSamples {
    private val values = HashMap<Any, Any>()
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> sample(key: Any, supplier: () -> T): T = values.getOrPut(key, supplier) as T
}
