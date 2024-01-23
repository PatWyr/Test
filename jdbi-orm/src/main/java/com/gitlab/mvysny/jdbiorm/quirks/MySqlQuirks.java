package com.gitlab.mvysny.jdbiorm.quirks;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.argument.Arguments;
import org.jdbi.v3.core.mapper.ColumnMappers;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    @Override
    public String offsetLimit(@Nullable Long offset, @Nullable Long limit) {
        if (offset != null && limit == null) {
            // MySQL/MariaDB requires both OFFSET and LIMIT to be present in the SQL but only offset was set.
            // hot-patch limit to some huge value.
            limit = (long) Integer.MAX_VALUE;
        }
        return Quirks.super.offsetLimit(offset, limit);
    }
}
