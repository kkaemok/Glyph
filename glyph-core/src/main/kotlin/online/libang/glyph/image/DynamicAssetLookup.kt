package online.libang.glyph.image

/** Immutable, precompiled asset table. Unknown keys cannot grow the cache. */
class DynamicAssetLookup<T : Any>(assets: Map<String, T>, private val fallback: T,
    private val missing: (String) -> Unit = {}) {
    private val assets = assets.toMap()
    private val reported = HashSet<String>()
    fun select(key: String): T = assets[key] ?: fallback.also {
        val report = synchronized(reported) { reported.size < 64 && reported.add(key) }
        if (report) missing(key)
    }
}
