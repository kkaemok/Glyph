package online.libang.glyph.api.manager;

import online.libang.glyph.api.database.HudDatabase;
import online.libang.glyph.api.database.HudDatabaseConnector;
import org.jetbrains.annotations.NotNull;

/**
 * Database manager.
 */
public interface DatabaseManager {
    /**
     * Gets current database.
     * @see online.libang.glyph.api.database.HudDatabaseConnector
     * @return current used database
     */
    @NotNull HudDatabase getCurrentDatabase();

    /**
     * Adds database connector.
     * @param name database's id
     * @param connector connector
     * @return whether to success
     */
    boolean addDatabase(@NotNull String name, @NotNull HudDatabaseConnector connector);
}
