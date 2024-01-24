package com.gitlab.mvysny.jdbiorm;

import org.intellij.lang.annotations.Language;
import org.jdbi.v3.core.statement.Query;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.gitlab.mvysny.jdbiorm.JdbiOrm.jdbi;

public class DaoOfJoin<T> extends DaoOfAny<T> {
    @NotNull
    private final String sql;

    public DaoOfJoin(@NotNull Class<T> entityClass, @NotNull @Language("sql") String sql) {
        super(entityClass);
        this.sql = Objects.requireNonNull(sql);
    }

    @Override
    public @NotNull List<T> findAll(@Nullable String orderBy, @Nullable Long offset, @Nullable Long limit) {
        if (limit != null && limit == 0L) {
            return new ArrayList<>();
        }
        checkOffsetLimit(offset, limit);
        final StringBuilder sql = new StringBuilder(this.sql);
        return jdbi().withHandle(handle -> {
                    if (orderBy != null) {
                        sql.append(" ORDER BY ").append(orderBy);
                    }
                    // H2 requires ORDER BY after LIMIT+OFFSET clauses.
                    appendOffsetLimit(sql, handle, offset, limit, orderBy != null);

                    return handle.createQuery(sql.toString())
                            .map(getRowMapper())
                            .list();
                }
        );
    }

    @NotNull
    @Override
    public List<T> findAllBy(@NotNull String where, @Nullable String orderBy,
                             @Nullable final Long offset, @Nullable final Long limit,
                             @NotNull Consumer<Query> queryConsumer) {
        Objects.requireNonNull(where, "where");
        Objects.requireNonNull(queryConsumer, "queryConsumer");
        if (limit != null && limit == 0L) {
            return new ArrayList<>();
        }
        final StringBuilder sql = new StringBuilder(this.sql);
        sql.append(" WHERE ").append(where);
        if (orderBy != null) {
            sql.append(" ORDER BY ").append(orderBy);
        }
        checkOffsetLimit(offset, limit);
        return jdbi().withHandle(handle -> {
                    appendOffsetLimit(sql, handle, offset, limit, orderBy != null);
                    final Query query = handle.createQuery(sql.toString());
                    queryConsumer.accept(query);
                    return query
                            .map(getRowMapper())
                            .list();
                }
        );
    }
}
