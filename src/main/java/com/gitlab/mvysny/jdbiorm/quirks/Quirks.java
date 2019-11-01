package com.gitlab.mvysny.jdbiorm.quirks;

import org.jdbi.v3.core.Handle;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Every database has its quirks. This object serves the purpose of configuring
 * JDBI for certain databases in order for JDBI to work properly.
 * <p></p>
 * Quirks to apply to {@link Handle}. See {@link DatabaseQuirksDetectorJdbiPlugin}
 * for the auto-detection algorithm.
 * @author mavi
 */
public interface Quirks {
    /**
     * Checks if this quirks plugin should activate for given connection.
     * @param connection the JDBC connection to check
     * @return true if this quirks should be applied.
     * @throws SQLException on SQL exception
     */
    boolean shouldActivate(@NotNull Connection connection) throws SQLException;

    /**
     * Configures JDBI handle once when it's created, before a query/update is attempted.
     * @param handle the handle to configure, not null.
     */
    void configure(@NotNull Handle handle);

    /**
     * Default implementation which does nothing and {@link #shouldActivate(Connection)} always returns false.
     */
    Quirks NO_QUIRKS = new Quirks() {
        @Override
        public boolean shouldActivate(@NotNull Connection connection) {
            return false;
        }

        @Override
        public void configure(@NotNull Handle handle) {
        }
    };
}
