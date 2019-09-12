package com.gitlab.mvysny.jdbiorm.quirks;

import com.gitlab.mvysny.jdbiorm.JdbiOrm;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.spi.JdbiPlugin;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * @author mavi
 */
public class DatabaseQuirksDetectorJdbiPlugin extends JdbiPlugin.Singleton {

    @NotNull
    private static Quirks findQuirksFor(@NotNull Connection dataSource) throws SQLException {
        if (JdbiOrm.quirks != null) {
            return JdbiOrm.quirks;
        }
        for (Quirks quirks : Quirks.ALL_QUIRKS) {
            if (quirks.shouldActivate(dataSource)) {
                return quirks;
            }
        }
        return Quirks.NO_QUIRKS;
    }

    @Override
    public Handle customizeHandle(Handle handle) throws SQLException {
        final Quirks quirks = findQuirksFor(handle.getConnection());
        quirks.configure(handle);
        return handle;
    }
}
