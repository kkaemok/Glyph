package online.libang.glyph.command

import online.libang.glyph.api.popup.Popup
import java.util.Collections

class PopupStack(
    private val popupList: Collection<Popup>
) : Iterable<Popup> {
    override fun iterator(): Iterator<Popup> = Collections.unmodifiableCollection(popupList).iterator()
}