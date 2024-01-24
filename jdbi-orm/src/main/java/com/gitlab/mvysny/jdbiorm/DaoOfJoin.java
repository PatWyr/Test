package com.gitlab.mvysny.jdbiorm;

import org.intellij.lang.annotations.Language;
import org.jdbi.v3.core.result.ResultIterable;
import org.jdbi.v3.core.statement.Query;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
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
    protected <R> R findAllBy(@Nullable String where, @Nullable String orderBy, @Nullable Long offset, @Nullable Long limit, @NotNull Consumer<Query> queryConsumer, @NotNull Function<ResultIterable<T>, R> iterableMapper, @Nullable R empty) {
        Objects.requireNonNull(queryConsumer, "queryConsumer");
        if (limit != null && limit == 0L) {
            return empty;
        }
        final StringBuilder sql = new StringBuilder(this.sql);
        if (where != null) {
            sql.append(" WHERE ").append(where);
        }
        if (orderBy != null) {
            sql.append(" ORDER BY ").append(orderBy);
        }
        checkOffsetLimit(offset, limit);
        return jdbi().withHandle(handle -> {
                    appendOffsetLimit(sql, handle, offset, limit, orderBy != null);
                    final Query query = handle.createQuery(sql.toString());
                    queryConsumer.accept(query);
                    final ResultIterable<T> resultIterable = query
                            .map(getRowMapper());
                    return iterableMapper.apply(resultIterable);
                }
        );
    }
}
