package com.gitlab.mvysny.jdbiorm.quirks;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.config.JdbiConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
     * Returns the SQL string such as <code>LIMIT 10 OFFSET 20</code>.
     * @param offset fetch rows from this offset
     * @param limit fetch at most this amount of rows
     * @return the string <code>LIMIT 10 OFFSET 20</code> for
     */
    default String offsetLimit(@Nullable Long offset, @Nullable Long limit) {
        String result = "";
        if (limit != null) {
            result += " LIMIT " + limit;
        }
        if (offset != null) {
            result += " OFFSET " + offset;
        }
        return result;
    }

    @Nullable
    default String offsetLimitRequiresOrderBy() {
        return null;
    }

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

    /**
     * Returns quirks for a database for given handle.
     * @param handle the handle, not null.
     * @return quirks, not null.
     */
    @NotNull
    static Quirks from(@NotNull Handle handle) {
        return handle.getConfig(Holder.class).quirks;
    }

    class Holder implements JdbiConfig<Holder> {
        @NotNull
        public Quirks quirks;

        public Holder() {
            this(NO_QUIRKS);
        }

        public Holder(@NotNull Quirks quirks) {
            this.quirks = quirks;
        }

        @Override
        public Holder createCopy() {
            return new Holder(quirks);
        }
    }
}
