package online.libang.glyph.api.manager;

import online.libang.glyph.api.listener.HudListener;
import online.libang.glyph.api.update.UpdateEvent;
import online.libang.glyph.api.yaml.YamlObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Set;
import java.util.function.Function;

/**
 * Represents listener manager.
 */
public interface ListenerManager {
    /**
     * Adds listener builder.
     * @param name listener name
     * @param listenerFunction builder
     */
    void addListener(@NotNull String name, @NotNull Function<YamlObject, Function<UpdateEvent, HudListener>> listenerFunction);

    /**
     * Gets all listener names
     * @return listener name
     */
    @NotNull
    @Unmodifiable
    Set<String> getAllListenerKeys();
}
