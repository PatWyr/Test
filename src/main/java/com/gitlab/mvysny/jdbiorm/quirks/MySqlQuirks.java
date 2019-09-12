package com.gitlab.mvysny.jdbiorm.quirks;

import org.jdbi.v3.core.argument.Arguments;
import org.jdbi.v3.core.config.Configurable;
import org.jdbi.v3.core.mapper.ColumnMappers;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * @author mavi
 */
public class MySqlQuirks implements Quirks {
    @Override
    public boolean shouldActivate(@NotNull Connection connection) throws SQLException {
        final String databaseProductName = connection.getMetaData().getDatabaseProductName();
        return databaseProductName.contains("MariaDB") || databaseProductName.contains("MySQL");
    }

    @Override
    public void configure(@NotNull Configurable<?> jdbi) {
        jdbi.getConfig(Arguments.class).register(new MySqlArgumentFactory());
        jdbi.getConfig(ColumnMappers.class).register(new MySqlColumnMapperFactory());
    }
}
