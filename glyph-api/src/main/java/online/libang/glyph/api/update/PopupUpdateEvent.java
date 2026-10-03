package online.libang.glyph.api.update;

import online.libang.glyph.api.popup.PopupIterator;
import org.jetbrains.annotations.NotNull;

/**
 * A handled updated event by Popup
 * @see online.libang.glyph.api.popup.Popup
 * @param source source
 * @param iterator popup iterator
 */
public record PopupUpdateEvent(@NotNull UpdateEvent source, @NotNull PopupIterator iterator) implements UpdateEvent {
    @Override
    public @NotNull UpdateReason getType() {
        return source.getType();
    }

    @Override
    public @NotNull Object getKey() {
        return source.getKey();
    }
}
