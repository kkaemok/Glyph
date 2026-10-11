package online.libang.glyph.popup

import kr.toxicity.hud.api.component.WidthComponent
import kr.toxicity.hud.api.popup.PopupIterator
import kr.toxicity.hud.api.popup.PopupIteratorGroup

/** Coordinates publication and retirement with async callers of Popup.show(). */
internal object PopupGroupLifecycle {
    fun add(groups: MutableMap<String, PopupIteratorGroup>, name: String, iterator: PopupIterator) {
        groups.compute(name) { _, current ->
            (current ?: PopupIteratorGroupImpl()).apply { addIterator(iterator) }
        }
    }

    fun render(groups: MutableMap<String, PopupIteratorGroup>): List<WidthComponent> {
        val result = ArrayList<WidthComponent>()
        for (name in groups.keys) {
            // Use the same per-key operation as publication: a new iterator must not
            // be added to a group after the render thread has retired it.
            groups.computeIfPresent(name) { _, group ->
                result += group.next()
                // An empty frame may still have queued or temporarily invisible popups.
                group.takeIf { it.index > 0 }
            }
        }
        return result
    }
}
