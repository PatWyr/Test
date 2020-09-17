package com.gitlab.mvysny.jdbiorm.quirks;

import org.jdbi.v3.core.Handle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Adds MySQL and MariaDB quirks support.
 * @author Martin Vysny <mavi@vaadin.com>
 */
public class MssqlQuirks implements Quirks {
    @Override
    public boolean shouldActivate(@NotNull Connection connection) throws SQLException {
        final String databaseProductName = connection.getMetaData().getDatabaseProductName();
        return databaseProductName.contains("Microsoft SQL Server");
    }

    @Override
    public void configure(@NotNull Handle handle) {
    }

    @Override
    public String offsetLimit(@Nullable Long offset, @Nullable Long limit) {
        if (limit != null && limit == 0L) {
            throw new IllegalArgumentException("Parameter limit: invalid value " + limit + ": must be 1 or greater");
        }
        String result = "";
        if (offset == null && limit != null) {
            offset = 0L;
        }
        if (offset != null) {
            result += " OFFSET " + offset + " ROWS";
        }
        if (limit != null) {
            result += " FETCH NEXT " + limit + " ROWS ONLY";
        }
        return result;
    }

    @Override
    @Nullable
    public String offsetLimitRequiresOrderBy() {
        return "ORDER BY (SELECT 1)";
    }
}
