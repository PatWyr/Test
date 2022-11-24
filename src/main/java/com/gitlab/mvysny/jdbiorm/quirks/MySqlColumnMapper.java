package com.gitlab.mvysny.jdbiorm.quirks;

import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jetbrains.annotations.NotNull;
import sun.reflect.generics.tree.TypeSignature;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.UUID;

/**
 * Converts MySQL UUID (binary(16)) to UUID.
 * @author mavi
 */
public class MySqlColumnMapper implements ColumnMapper<UUID> {

    @Override
    public UUID map(@NotNull ResultSet r, int columnNumber, StatementContext ctx) throws SQLException {
        final int columnType = r.getMetaData().getColumnType(columnNumber);
        // MariaDB: if the column is defined as "id binary(16) primary key", the
        // column type will be -3 VARBINARY!
        if (columnType == Types.BINARY || columnType == Types.VARBINARY) {
            final byte[] obj = r.getBytes(columnNumber);
            return obj == null ? null : uuidFromByteArray(obj);
        }
        final Object obj = r.getObject(columnNumber);
        if (obj == null) {
            return null;
        } else if (obj instanceof String) {
            final String str = (String) obj;
            return UUID.fromString(str);
        } else if (obj instanceof byte[]) {
            return uuidFromByteArray((byte[]) obj);
        } else {
            throw new IllegalArgumentException("Parameter obj: invalid value " + obj + ": cannot convert to UUID");
        }
    }

    @NotNull
    private static UUID uuidFromByteArray(@NotNull byte[] uuid) {
        final DataInputStream din = new DataInputStream(new ByteArrayInputStream(uuid));
        try {
            return new UUID(din.readLong(), din.readLong());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
