package com.gitlab.mvysny.jdbiorm.quirks;

import org.jdbi.v3.core.argument.AbstractArgumentFactory;
import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.statement.StatementContext;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Objects;
import java.util.UUID;

/**
 * Adds support for serializing {@link UUID} into MySQL's UUID type (which is just
 * {@code binary(16)}).
 * @author mavi
 */
public class MySqlUUIDArgumentFactory extends AbstractArgumentFactory<UUID> {
    // don't implement ArgumentFactory directly, it stopped working with JDBI 3.12.2

    protected MySqlUUIDArgumentFactory() {
        super(Types.BINARY);
    }

    @Override
    protected Argument build(UUID value, ConfigRegistry config) {
        return new MySqlUUIDArgument(value);
    }

    private static class MySqlUUIDArgument implements Argument {
        @NotNull
        private final UUID uuid;

        public MySqlUUIDArgument(@NotNull UUID uuid) {
            this.uuid = Objects.requireNonNull(uuid, "uuid");
        }

        @Override
        public void apply(int position, PreparedStatement statement, StatementContext ctx) throws SQLException {
            statement.setBytes(position, uuidToByteArray(uuid));
        }

        @Override
        public String toString() {
            return "MySqlUUIDArgument{" + uuid + '}';
        }
    }

    private static byte[] uuidToByteArray(@NotNull UUID uuid) {
        final ByteArrayOutputStream out = new ByteArrayOutputStream(8);
        final DataOutputStream dout = new DataOutputStream(out);
        try {
            dout.writeLong(uuid.getMostSignificantBits());
            dout.writeLong(uuid.getLeastSignificantBits());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return out.toByteArray();
    }
}
