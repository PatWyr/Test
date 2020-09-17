package com.gitlab.mvysny.jdbiorm.quirks;

import org.jdbi.v3.core.Handle;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * The database vendor.
 * @author mavi
 */
public enum DatabaseVariant {
    MySQLMariaDB {
        @NotNull
        @Override
        public Quirks getQuirks() {
            return new MySqlQuirks();
        }

        @Override
        public boolean matches(@NotNull Connection connection) throws SQLException {
            final String databaseProductName = connection.getMetaData().getDatabaseProductName();
            return databaseProductName.contains("MariaDB") || databaseProductName.contains("MySQL");
        }
    },
    PostgreSQL {
        @Override
        public boolean matches(@NotNull Connection connection) throws SQLException {
            final String databaseProductName = connection.getMetaData().getDatabaseProductName();
            return databaseProductName.contains("PostgreSQL");
        }
    },
    H2 {
        @Override
        public boolean matches(@NotNull Connection connection) throws SQLException {
            final String databaseProductName = connection.getMetaData().getDatabaseProductName();
            return databaseProductName.equals("H2");
        }
    },
    MSSQL {
        @NotNull
        @Override
        public Quirks getQuirks() {
            return new MssqlQuirks();
        }

        @Override
        public boolean matches(@NotNull Connection connection) throws SQLException {
            final String databaseProductName = connection.getMetaData().getDatabaseProductName();
            return databaseProductName.contains("Microsoft SQL Server");
        }
    },
    Unknown {
        @Override
        public boolean matches(@NotNull Connection connection) {
            return true;
        }
    },
    ;

    @NotNull
    public Quirks getQuirks() {
        return Quirks.NO_QUIRKS;
    }

    public abstract boolean matches(@NotNull Connection connection) throws SQLException;

    /**
     * Auto-detects the database variant.
     *
     * @param connection the JDBC connection, not null.
     * @return database variant, not null.
     */
    @NotNull
    public static DatabaseVariant detect(@NotNull Connection connection) throws SQLException {
        for (DatabaseVariant variant : values()) {
            if (variant.matches(connection)) {
                return variant;
            }
        }
        return Unknown;
    }

    /**
     * Returns the database variant for given handle.
     * @param handle the handle, not null.
     * @return the variant, not null.
     */
    @NotNull
    public static DatabaseVariant from(@NotNull Handle handle) {
        return handle.getConfig(Quirks.Holder.class).variant;
    }
}
