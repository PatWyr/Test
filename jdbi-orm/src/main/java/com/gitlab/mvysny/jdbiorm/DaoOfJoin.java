package com.gitlab.mvysny.jdbiorm;

import org.intellij.lang.annotations.Language;
import org.jdbi.v3.core.result.ResultIterable;
import org.jdbi.v3.core.statement.Query;
import org.jdbi.v3.core.statement.Update;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.gitlab.mvysny.jdbiorm.JdbiOrm.jdbi;

/**
 * A DAO which maps an outcome of a JOIN statement (or any SQL statement in general).
 * DaoOfJoin will automatically append "WHERE", "ORDER BY", "OFFSET" and "LIMIT" clauses at the end of your SQL statement, so
 * you have to omit those in your SQL statement.
 * @param <T> the type of the Java POJO entities being returned.
 */
public class DaoOfJoin<T> extends DaoOfAny<T> {
    /**
     * The part of the SQL statement which may be appended with WHERE / ORDER BY / OFFSET / LIMIT stanzas.
     */
    @NotNull
    private final String sql;

    /**
     * Creates the DAO.
     * @param pojoClass the class of Java POJO to which the result will be mapped. The class doesn't need to implement anything, not even Serializable.
     *                  JDBI is still used to map the JDBC resultset to the fields of the class; therefore the fields
     *                  may be annotated with {@link org.jdbi.v3.core.mapper.Nested @Nested}, {@link org.jdbi.v3.core.annotation.JdbiProperty} and other annotations
     *                  to configure the mapping. FieldMapper is used by default.
     * @param sql the part of the SQL statement which may be appended with WHERE / ORDER BY / OFFSET / LIMIT stanzas.
     */
    public DaoOfJoin(@NotNull Class<T> pojoClass, @NotNull @Language("sql") String sql) {
        super(pojoClass);
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

    @Override
    public boolean existsAny() {
        return count() > 0;
    }

    @Override
    public boolean existsBy(@NotNull String where, @NotNull Consumer<Query> queryConsumer) {
        return countBy(where, queryConsumer) > 0;
    }

    @Override
    public void deleteBy(@NotNull String where, @NotNull Consumer<Update> updateConsumer) {
        throw new UnsupportedOperationException("DaoOfJoin doesn't support deletion by default");
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" + entityClass.getSimpleName() + ": '" + sql + "'}";
    }
}
