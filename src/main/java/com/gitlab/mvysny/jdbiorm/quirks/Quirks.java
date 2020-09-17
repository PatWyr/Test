package com.gitlab.mvysny.jdbiorm.quirks;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.config.JdbiConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Every database has its quirks. This object serves the purpose of configuring
 * JDBI for certain databases in order for JDBI to work properly.
 * <p></p>
 * Quirks to apply to {@link Handle}. See {@link DatabaseQuirksDetectorJdbiPlugin}
 * for the auto-detection algorithm.
 *
 * @author mavi
 */
public interface Quirks {
    /**
     * Configures JDBI handle once when it's created, before a query/update is attempted.
     *
     * @param handle the handle to configure, not null.
     */
    void configure(@NotNull Handle handle);

    /**
     * Returns the SQL string such as <code>LIMIT 10 OFFSET 20</code>.
     *
     * @param offset fetch rows from this offset
     * @param limit  fetch at most this amount of rows
     * @return the string <code>LIMIT 10 OFFSET 20</code> which generally works
     * in all databases.
     */
    default String offsetLimit(@Nullable Long offset, @Nullable Long limit) {
        // MariaDB requires LIMIT first, then OFFSET: https://mariadb.com/kb/en/library/limit/
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
     * Default implementation which does nothing.
     */
    Quirks NO_QUIRKS = new Quirks() {
        @Override
        public void configure(@NotNull Handle handle) {
        }
    };

    /**
     * Returns quirks for a database for given handle.
     *
     * @param handle the handle, not null.
     * @return quirks, not null.
     */
    @NotNull
    public static Quirks from(@NotNull Handle handle) {
        return handle.getConfig(Holder.class).quirks;
    }

    class Holder implements JdbiConfig<Holder> {
        @NotNull
        public Quirks quirks;
        @NotNull
        public DatabaseVariant variant;

        public Holder() {
            this(NO_QUIRKS, DatabaseVariant.Unknown);
        }

        public Holder(@NotNull Quirks quirks, @NotNull DatabaseVariant databaseVariant) {
            this.quirks = quirks;
            this.variant = databaseVariant;
        }

        @Override
        public Holder createCopy() {
            return new Holder(quirks, variant);
        }
    }
}
