package online.libang.glyph.renderer

import online.libang.glyph.api.state.HudState

/** Bounded cache owned by one compiled segment and one player renderer. */
internal class DependencyCache<T>(private val dependencies: List<String>) {
    private var versions: LongArray? = null
    private var value: T? = null
    @Suppress("UNCHECKED_CAST")
    fun get(snapshot: HudState.Snapshot, force: Boolean = false, render: () -> T): T {
        val previous = versions
        var dirty = force || previous == null
        if (!dirty) for (i in dependencies.indices) {
            if (previous!![i] != snapshot.version(dependencies[i])) { dirty = true; break }
        }
        if (!dirty) return value as T
        val rendered = render() // Failed rendering does not advance the cache.
        versions = LongArray(dependencies.size) { snapshot.version(dependencies[it]) }
        value = rendered
        return rendered
    }
}
