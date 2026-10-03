package online.libang.glyph.api;

import kr.toxicity.command.BetterCommandSource;
import online.libang.glyph.api.manager.*;
import online.libang.glyph.api.plugin.ReloadFlagType;
import online.libang.glyph.api.plugin.ReloadInfo;
import online.libang.glyph.api.plugin.ReloadState;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Represents the main class of Glyph.
 */
@SuppressWarnings("unused")
public interface Glyph {
    /** Returns the screen-effect capability for this player. */
    default online.libang.glyph.api.effect.PostEffects postEffects(online.libang.glyph.api.player.HudPlayer player) {
        return bootstrap().postEffects(player);
    }

    /**
     * Default namespace.
     */
    String DEFAULT_NAMESPACE = "glyph";
    /**
     * Adventure api version.
     */
    String ADVENTURE_VERSION = "5.2.0";
    /**
     * Examination version.
     */
    String EXAMINATION_VERSION = "1.3.0";

    /**
     * BStats id for Bukkit
     */
    int BSTATS_ID_BUKKIT = 21287;
    /**
     * BStats id for Velocity
     */
    int BSTATS_ID_VELOCITY = 23460;


    /**
     * Supports legacy api.
     * @return Glyph main instance.
     */
    static @NotNull Glyph getInstance() {
        return GlyphAPI.inst();
    }

    /**
     * Executes reload.
     * @param args reload args
     * @return result of reload.
     */
    default @NotNull ReloadState reload(@NotNull ReloadFlagType... args) {
        return reload(bootstrap().consoleSource(), args);
    }

    /**
     * Executes reload.
     * @param sender log handler.
     * @param args reload args.
     * @return result of reload.
     */
    default @NotNull ReloadState reload(@NotNull BetterCommandSource sender, @NotNull ReloadFlagType... args) {
        return reload(new ReloadInfo(sender, new HashSet<>(List.of(args))));
    }

    /**
     * Checks whether this build is a dev version.
     * @return whether this build is a dev version
     */
    boolean isDevVersion();

    /**
     * Gets a bootstrap.
     * @return bootstrap.
     */
    @NotNull GlyphBootstrap bootstrap();


    /**
     * Executes reload.
     * @param info reload info.
     * @return result of reload.
     */
    @NotNull ReloadState reload(@NotNull ReloadInfo info);


    /**
     * Returns whether this plugin merges the first bossbar.
     * @return whether to merge
     */
    boolean isMergeBossBar();

    /**
     * Loads plugin's resource to some directory
     * @param prefix resource folder
     * @param dir target directory
     */
    void loadAssets(@NotNull String prefix, @NotNull File dir);
    /**
     * Loads plugin's resource to some directory
     * @param prefix resource folder
     * @param consumer for each callback
     */
    void loadAssets(@NotNull String prefix, @NotNull BiConsumer<String, InputStream> consumer);

    /**
     * Gets the width of default font's char
     * @param codepoint target codepoint
     * @return width
     */
    int getWidth(int codepoint);

    /**
     * Gets placeholder manager.
     * @return placeholder manager
     */
    @NotNull PlaceholderManager getPlaceholderManager();
    /**
     * Gets listener manager.
     * @return listener manager
     */
    @NotNull ListenerManager getListenerManager();
    /**
     * Gets popup manager.
     * @return popup manager
     */
    @NotNull PopupManager getPopupManager();
    /**
     * Gets config manager.
     * @return config manager
     */
    @NotNull ConfigManager getConfigManager();
    /**
     * Gets compass manager.
     * @return compass manager
     */
    @NotNull CompassManager getCompassManager();
    /**
     * Gets trigger manager.
     * @return trigger manager
     */
    @NotNull TriggerManager getTriggerManager();
    /**
     * Gets hud manager.
     * @return hud manager
     */
    @NotNull HudManager getHudManager();
    /**
     * Gets database manager.
     * @return database manager
     */
    @NotNull DatabaseManager getDatabaseManager();
    /**
     * Gets shader manager.
     * @return shader manager
     */
    @NotNull ShaderManager getShaderManager();
    /**
     * Gets player manager.
     * @return player manager
     */
    @NotNull PlayerManager getPlayerManager();
    /**
     * Gets text manager.
     * @return text manager
     */
    @NotNull TextManager getTextManager();

    /**
     * Returns this plugin is currently on reload.
     * @return whether to this plugin is on reload
     */
    boolean isOnReload();

    /**
     * Returns encoded namespace.
     * @return namespace.
     */
    @NotNull String getEncodedNamespace();

    /**
     * Returns default font key.
     * @return font key
     */
    @NotNull Key getDefaultKey();


    /**
     * Get a translated result of a key.
     * @param locale locale
     * @param key key
     * @return translated value
     */
    @Nullable String translate(@NotNull String locale, @NotNull String key);

    /**
     * Adds some task when reload is started.
     * @param runnable reload start task
     */
    void addReloadStartTask(@NotNull Runnable runnable);

    /**
     * Adds some task when reload is ended.
     * @param runnable reload end task
     */
    void addReloadEndTask(@NotNull Consumer<ReloadState> runnable);
}
