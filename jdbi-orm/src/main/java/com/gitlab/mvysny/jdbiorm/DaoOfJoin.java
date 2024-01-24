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
            sql.append(" WHERE <WHERE>");
        }
        if (orderBy != null) {
            sql.append(" ORDER BY ").append(orderBy);
        }
        checkOffsetLimit(offset, limit);
        return jdbi().withHandle(handle -> {
                    appendOffsetLimit(sql, handle, offset, limit, orderBy != null);
                    final Query query = handle.createQuery(sql.toString());
                    if (where != null) {
                        query.define("WHERE", where);
                    }
                    queryConsumer.accept(query);
                    final ResultIterable<T> resultIterable = query
                            .map(getRowMapper());
                    return iterableMapper.apply(resultIterable);
                }
        );
    }

    @Override
    public void deleteAll() {
        throw new UnsupportedOperationException("DaoOfJoin doesn't support deletion by default");
    }

    @Override
    public long countBy(@Nullable String where, @NotNull Consumer<Query> queryConsumer) {
        final StringBuilder sb = new StringBuilder(this.sql);
        if (where != null) {
            sb.append(" WHERE <WHERE>");
        }
        // previously, the count was obtained by a dirty trick - the ResultSet was simply scrolled to the last line and the row number is obtained.
        // however, PostgreSQL doesn't seem to like this: https://github.com/mvysny/vaadin-on-kotlin/issues/19
        // anyway there is a better way: simply wrap the select with "SELECT count(*) FROM (select)"
        // subquery in FROM must have an alias
        final String sql = "SELECT count(*) FROM (" + sb + ") AS Foo";
        return jdbi().withHandle(handle -> {
            final Query query = handle.createQuery(sql)
                    .define("TABLE", meta.getDatabaseTableName());
            if (where != null) {
                query.define("WHERE", where);
            }
            queryConsumer.accept(query);
            return query.mapTo(Long.class).one();
        });
    }
}
