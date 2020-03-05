package com.gitlab.mvysny.jdbiorm.quirks;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.argument.Arguments;
import org.jdbi.v3.core.mapper.ColumnMappers;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;

/**
 * Adds MySQL and MariaDB quirks support.
 * @author mavi
 */
public class MySqlQuirks implements Quirks {
    @Override
    public boolean shouldActivate(@NotNull Connection connection) throws SQLException {
        final String databaseProductName = connection.getMetaData().getDatabaseProductName();
        return databaseProductName.contains("MariaDB") || databaseProductName.contains("MySQL");
    }

    @Override
    public void configure(@NotNull Handle handle) {
        handle.getConfig(Arguments.class).register(new MySqlUUIDArgumentFactory());
        handle.getConfig(ColumnMappers.class).register(UUID.class, new MySqlColumnMapper());
    }
}
