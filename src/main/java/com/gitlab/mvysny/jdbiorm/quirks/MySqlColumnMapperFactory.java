package com.gitlab.mvysny.jdbiorm.quirks;

import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.mapper.ColumnMapperFactory;
import org.jdbi.v3.core.statement.StatementContext;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.lang.reflect.Type;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

/**
 * Converts MySQL UUID (binary(16)) to UUID.
 * @author mavi
 */
public class MySqlColumnMapperFactory implements ColumnMapperFactory {
    @Override
    public Optional<ColumnMapper<?>> build(Type type, ConfigRegistry config) {
        if (type.equals(UUID.class)) {
            return Optional.of(new MySqlColumnMapper());
        }
        return Optional.empty();
    }

    private static class MySqlColumnMapper implements ColumnMapper<UUID> {

        @Override
        public UUID map(ResultSet r, int columnNumber, StatementContext ctx) throws SQLException {
            final Object obj = r.getObject(columnNumber);
            if (obj == null) {
                return null;
            } else if (obj instanceof String) {
                return UUID.fromString((String) obj);
            } else if (obj instanceof byte[]) {
                return uuidFromByteArray((byte[]) obj);
            } else {
                throw new IllegalArgumentException("Parameter obj: invalid value " + obj + ": cannot convert to UUID");
            }
        }
    }

    private static UUID uuidFromByteArray(@NotNull byte[] uuid) {
        final DataInputStream din = new DataInputStream(new ByteArrayInputStream(uuid));
        try {
            return new UUID(din.readLong(), din.readLong());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
