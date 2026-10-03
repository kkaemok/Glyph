package online.libang.glyph.api.configuration;

import online.libang.glyph.api.GlyphAPI;
import online.libang.glyph.api.compass.Compass;
import online.libang.glyph.api.hud.Hud;
import online.libang.glyph.api.player.HudPlayer;
import online.libang.glyph.api.popup.Popup;
import online.libang.glyph.api.update.UpdateEvent;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * A type of HUD element
 * @see HudObject
 * @param clazz object's class
 * @param name object's name
 * @param function object's render function
 * @param <T> object type
 */
public record HudObjectType<T extends HudObject>(
        @NotNull Class<T> clazz,
        @NotNull String name,
        @NotNull BiFunction<T, HudPlayer, HudComponentSupplier<T>> function,
        @NotNull Function<String, T> stringGetter,
        @NotNull Supplier<Collection<T>> valuesSupplier
) {
    private static final Map<String, HudObjectType<?>> TYPE_MAP = new HashMap<>();
    private static final Map<String, HudObjectType<?>> TYPE_MAP_VIEW = Collections.unmodifiableMap(TYPE_MAP);

    /**
     * Hud type
     * @see Hud
     */
    public static final HudObjectType<Hud> HUD = new HudObjectType<>(
            Hud.class,
            "hud",
            Hud::createRenderer,
            GlyphAPI.inst().getHudManager()::getHud,
            GlyphAPI.inst().getHudManager()::getAllHuds
    );

    /**
     * Popup type
     * @see Popup
     */
    public static final HudObjectType<Popup> POPUP = new HudObjectType<>(
            Popup.class,
            "popup",
            (popup, player) -> HudComponentSupplier.of(popup, () -> {
                popup.show(UpdateEvent.EMPTY, player);
                return Collections.emptyList();
            }),
            GlyphAPI.inst().getPopupManager()::getPopup,
            GlyphAPI.inst().getPopupManager()::getAllPopups
    );
    /**
     * Compass type
     * @see Compass
     */
    public static final HudObjectType<Compass> COMPASS = new HudObjectType<>(
            Compass.class,
            "compass",
            Compass::indicate,
            GlyphAPI.inst().getCompassManager()::getCompass,
            GlyphAPI.inst().getCompassManager()::getAllCompasses
    );

    public HudObjectType {
        TYPE_MAP.put(name, this);
    }

    @NotNull
    @Unmodifiable
    public static Collection<HudObjectType<?>> types() {
        return TYPE_MAP_VIEW.values();
    }

    public @Nullable T byName(@NotNull String name) {
        return stringGetter.apply(name);
    }

    @NotNull
    @Unmodifiable
    public Collection<T> all() {
        return valuesSupplier.get();
    }

    @NotNull
    public Stream<T> defaultObjects() {
        return all().stream().filter(HudObject::isDefault);
    }

    /**
     * Gets supplier from an object
     * @param object target object
     * @param player target player
     * @return supplier
     */
    @ApiStatus.Internal
    public @NotNull HudComponentSupplier<?> invoke(@NotNull HudObject object, @NotNull HudPlayer player) {
        try {
            return function.apply(clazz.cast(object), player);
        } catch (Throwable e) {
            return HudComponentSupplier.empty(object);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof HudObjectType<?> that)) return false;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name);
    }
}
