package com.gitlab.mvysny.jdbiorm.quirks;

import org.jdbi.v3.core.Handle;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

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
     * @throws SQLException
     */
    boolean shouldActivate(@NotNull Connection connection) throws SQLException;
    void configure(@NotNull Handle handle);

    Quirks NO_QUIRKS = new Quirks() {
        @Override
        public boolean shouldActivate(@NotNull Connection connection) throws SQLException {
            return false;
        }

        @Override
        public void configure(@NotNull Handle handle) {
        }
    };

    /**
     * @todo mavi use service loader
     */
    List<Quirks> ALL_QUIRKS = Arrays.asList(new MySqlQuirks());
}
