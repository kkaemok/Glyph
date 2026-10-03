package online.libang.glyph.api.database;

import online.libang.glyph.api.yaml.YamlObject;
import org.jetbrains.annotations.NotNull;

/**
 * A connector of database.
 */
public interface HudDatabaseConnector {
    /**
     * Tries to connect
     * @throws RuntimeException if connection has failed.
     * @see HudDatabase
     * @param section connection information
     * @return connected database
     */
    @NotNull HudDatabase connect(@NotNull YamlObject section);
}
