package com.gitlab.mvysny.jdbiorm.quirks;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.argument.Arguments;
import org.jdbi.v3.core.mapper.ColumnMappers;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Adds MySQL and MariaDB quirks support.
 * @author mavi
 */
public class MySqlQuirks implements Quirks {
    @Override
    public void configure(@NotNull Handle handle) {
        handle.getConfig(Arguments.class).register(new MySqlUUIDArgumentFactory());
        handle.getConfig(ColumnMappers.class).register(UUID.class, new MySqlColumnMapper());
    }
}
