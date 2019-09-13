package com.gitlab.mvysny.jdbiorm;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.mapper.reflect.FieldMapper;
import org.jdbi.v3.core.result.ResultIterable;
import org.jdbi.v3.core.result.ResultIterator;
import org.jdbi.v3.core.statement.Binding;
import org.jdbi.v3.core.statement.Query;
import org.jdbi.v3.core.statement.SqlStatement;
import org.jdbi.v3.core.statement.Update;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.gitlab.mvysny.jdbiorm.JdbiOrm.jdbi;

/**
 * Sometimes you don't want a class to be an entity for some reason (e.g. when it doesn't have a primary key),
 * but still it's mapped to a table and you would want to have Dao support for that class.
 * Just let your class have a static field, for example:
 * <pre>
 * public class Log {
 *   public static final DaoOfAny&lt;Log> dao = new DaoOfAny&lt>(Log.class);
 * }
 * </pre>
 * You can now use `Log.dao.findAll()`, `Log.dao.count()` and other nice methods.
 * <p></p>
 * Use {@link Table} to change the database table name (by default it's the class
 * simple name); use {@link org.jdbi.v3.core.mapper.reflect.ColumnName} to set
 * the database column name.
 * @param <T> the type of the class provided by this Dao
 */
public class DaoOfAny<T> {
    @NotNull
    protected final Class<T> entityClass;
    @NotNull
    protected final EntityMeta meta;

    public DaoOfAny(@NotNull Class<T> entityClass) {
        this.entityClass = Objects.requireNonNull(entityClass);
        meta = new EntityMeta(entityClass);
    }

    @NotNull
    protected RowMapper<T> getRowMapper() {
        return FieldMapper.of(entityClass);
    }

    /**
     * Finds all rows in given table. Fails if there is no table in the database with the
     * name of {@link EntityMeta#getDatabaseTableName()}. The list is eager
     * and thus it's useful for small-ish tables only.
     */
    @NotNull
    public List<T> findAll() {
        return jdbi().withHandle(handle -> handle.createQuery("select * from <TABLE>")
                .define("TABLE", meta.getDatabaseTableName())
                .map(getRowMapper())
                .list()
        );
    }

    /**
     * Finds all rows in given table. Fails if there is no table in the database with the
     * name of {@link EntityMeta#getDatabaseTableName()}. If both offset and limit
     * are specified, then the LIMIT and OFFSET sql paging is used.
     * @param offset start from this row. If not null, must be 0 or greater.
     * @param limit return this count of row at most. If not null, must be 0 or greater.
     */
    @NotNull
    public List<T> findAll(@Nullable final Long offset, @Nullable final Long limit) {
        final StringBuilder sql = new StringBuilder("select * from <TABLE>");
        if (offset != null && limit != null) {
            if (offset < 0) {
                throw new IllegalArgumentException("Parameter offset: invalid value " + offset + ": must be 0 or greater");
            }
            if (limit < 0) {
                throw new IllegalArgumentException("Parameter limit: invalid value " + limit + ": must be 0 or greater");
            }
            sql.append(" LIMIT " + Math.min(limit, Integer.MAX_VALUE) + " OFFSET " + Math.min(offset, Integer.MAX_VALUE));
        }
        return jdbi().withHandle(handle -> handle.createQuery(sql.toString())
                .define("TABLE", meta.getDatabaseTableName())
                .map(getRowMapper())
                .list()
        );
    }

    /**
     * Finds all matching rows in given table. Fails if there is no table in the database with the
     * name of {@link EntityMeta#getDatabaseTableName()}. If both offset and limit
     * are specified, then the LIMIT and OFFSET sql paging is used.
     * @param where the where clause, e.g. {@code name = :name}. Careful: this goes into the SQL as-is - could be misused for SQL injection!
     * @param offset start from this row. If not null, must be 0 or greater.
     * @param limit return this count of row at most. If not null, must be 0 or greater.
     */
    @NotNull
    public List<T> findAllBy(@NotNull String where, @Nullable final Long offset, @Nullable final Long limit, @NotNull Consumer<Query> queryConsumer) {
        final StringBuilder sql = new StringBuilder("select * from <TABLE> where <WHERE>");
        if (offset != null && limit != null) {
            if (offset < 0) {
                throw new IllegalArgumentException("Parameter offset: invalid value " + offset + ": must be 0 or greater");
            }
            if (limit < 0) {
                throw new IllegalArgumentException("Parameter limit: invalid value " + limit + ": must be 0 or greater");
            }
            sql.append(" LIMIT " + Math.min(limit, Integer.MAX_VALUE) + " OFFSET " + Math.min(offset, Integer.MAX_VALUE));
        }
        return jdbi().withHandle(handle -> {
                    final Query query = handle.createQuery(sql.toString())
                            .define("TABLE", meta.getDatabaseTableName())
                            .define("WHERE", where);
                    queryConsumer.accept(query);
                    return query
                            .map(getRowMapper())
                            .list();
                }
        );
    }

    /**
     * Retrieves single entity matching given {@code where} clause.
     * Returns null if there is no such entity; fails if there are two or more entities matching the criteria.
     * <p></p>
     * Example:
     * <pre>
     * Person.findOneBy("name = :name", q -> q.bind("name", "Albedo"))
     * </pre>
     * <p>
     * This function returns null if there is no item matching. Use {@link #getOneBy(String, Consumer)}
     * if you wish to return `null` in case that the entity does not exist.
     *
     * @param where the where clause, e.g. {@code name = :name}. Careful: this goes into the SQL as-is - could be misused for SQL injection!
     * @throws IllegalStateException if there are two or more matching entities.
     */
    @Nullable
    public T findOneBy(@NotNull String where, @NotNull Consumer<Query> queryConsumer) {
        return jdbi().withHandle(handle -> {
            final Query query = handle.createQuery("select * from <TABLE> where <WHERE>")
                    .define("TABLE", meta.getDatabaseTableName())
                    .define("WHERE", where);
            queryConsumer.accept(query);
            final ResultIterable<T> iterable = query.map(getRowMapper());
            return findOneFromIterable(iterable, () -> formatQuery(where, query));
        });
    }

    protected String formatQuery(@NotNull String sql, @NotNull SqlStatement<?> statement) {
        return entityClass.getSimpleName() + ": '" + sql + "'" + toString(statement);
    }

    /**
     * Retrieves single entity matching given {@code where} clause.
     * Fails if there is no such entity, or if there are two or more entities matching the criteria.
     * <p></p>
     * Example:
     * <pre>
     * Person.getOneBy("name = :name", q -> q.bind("name", "Albedo"))
     * </pre>
     * <p>
     * This function fails if there is no such entity or there are 2 or more. Use [findSpecificBy] if you wish to return `null` in case that
     * the entity does not exist.
     *
     * @param where the where clause, e.g. {@code name = :name}. Careful: this goes into the SQL as-is - could be misused for SQL injection!
     * @throws IllegalStateException if there is no entity matching given criteria, or if there are two or more matching entities.
     */
    @NotNull
    public T getOneBy(@NotNull String where, Consumer<Query> queryConsumer) {
        return jdbi().withHandle(handle -> {
                    final Query query = handle.createQuery("select * from <TABLE> where <WHERE>")
                            .define("TABLE", meta.getDatabaseTableName())
                            .define("WHERE", where);
                    queryConsumer.accept(query);
                    final ResultIterable<T> result = query.map(getRowMapper());
                    return getOneFromIterable(result, () -> formatQuery(where, query));
                }
        );
    }

    /**
     * Deletes all rows from this database table.
     */
    public void deleteAll() {
        jdbi().useHandle(handle -> handle.createUpdate("delete from <TABLE>")
                .define("TABLE", meta.getDatabaseTableName())
                .execute());
    }

    /**
     * Counts all rows in this table.
     */
    public long count() {
        return jdbi().withHandle(handle -> handle.createQuery("select count(*) from <TABLE>")
                .define("TABLE", meta.getDatabaseTableName())
                .mapTo(Long.class).one());
    }

    /**
     * Counts all matching rows in this table.
     *
     * @param where the where clause, e.g. {@code name = :name}. Careful: this goes into the SQL as-is - could be misused for SQL injection!
     */
    public long countBy(@NotNull String where, @NotNull Consumer<Query> queryConsumer) {
        return jdbi().withHandle(handle -> {
            final Query query = handle.createQuery("select count(*) from <TABLE> where <WHERE>")
                    .define("TABLE", meta.getDatabaseTableName())
                    .define("WHERE", where);
            queryConsumer.accept(query);
            return query.mapTo(Long.class).one();
        });
    }

    /**
     * Checks whether there exists any row in this table.
     */
    public boolean existsAny() {
        return jdbi().withHandle(handle -> handle.createQuery("select count(1) from <TABLE>")
                .define("TABLE", meta.getDatabaseTableName())
                .mapTo(Long.class).one() > 0);
    }

    /**
     * Checks whether there exists any row in this table.
     *
     * @param where the where clause, e.g. {@code name = :name}. Careful: this goes into the SQL as-is - could be misused for SQL injection!
     */
    public boolean existsBy(@NotNull String where, @NotNull Consumer<Query> queryConsumer) {
        return jdbi().withHandle(handle -> {
            final Query table = handle.createQuery("select count(1) from <TABLE> where <WHERE>")
                    .define("TABLE", meta.getDatabaseTableName())
                    .define("WHERE", where);
            queryConsumer.accept(table);
            return table.mapTo(Long.class).one() > 0;
        });
    }

    /**
     * Deletes rows matching given where clause.
     *
     * @param where the where clause, e.g. {@code name = :name}. Careful: this goes into the SQL as-is - could be misused for SQL injection!
     */
    public void deleteBy(@NotNull String where, @NotNull Consumer<Update> updateConsumer) {
        jdbi().withHandle(handle -> {
            final Update update = handle.createUpdate("delete from <TABLE> where <WHERE>")
                    .define("TABLE", meta.getDatabaseTableName())
                    .define("WHERE", where);
            updateConsumer.accept(update);
            return update.execute();
        });
    }

    @NotNull
    protected String toString(@NotNull SqlStatement<?> statement) {
        try {
            final Method getBinding = SqlStatement.class.getDeclaredMethod("getBinding");
            getBinding.setAccessible(true);
            final Binding binding = (Binding) getBinding.invoke(statement);
            return binding.toString();
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the only row in the result set, if any. Returns null if zero
     * rows are returned, or if the row itself is {@code null}.
     *
     * @param errorSupplier invoked in case of error; should list table name, the {@code where} clause
     *                      and parameters. Prepended by 'too many items matching '. Simply use
     *                      {@link #formatQuery(String, SqlStatement)}.
     * @return the only row in the result set, if any.
     * @throws IllegalStateException if the result set contains multiple rows
     */
    @Nullable
    protected T findOneFromIterable(@NotNull ResultIterable<T> iterable, @NotNull Supplier<String> errorSupplier) {
        try (ResultIterator<T> iter = iterable.iterator()) {
            if (!iter.hasNext()) {
                return null;
            }

            final T r = iter.next();

            if (iter.hasNext()) {
                throw new IllegalStateException("too many rows matching " + errorSupplier.get());
            }

            return r;
        }
    }

    /**
     * Returns the only row in the result set. Returns {@code null} if the row itself is
     * {@code null}.
     *
     * @param errorSupplier invoked in case of error; should list table name, the {@code where} clause
     *                      and parameters. Prepended by 'too many items matching '. Simply use
     *                      {@link #formatQuery(String, SqlStatement)} .
     * @return the only row in the result set.
     * @throws IllegalStateException if the result set contains zero or multiple rows
     */
    @NotNull
    protected T getOneFromIterable(ResultIterable<T> iterable, @NotNull Supplier<String> errorSupplier) {
        try (ResultIterator<T> iter = iterable.iterator()) {
            if (!iter.hasNext()) {
                throw new IllegalStateException("no row matching " + errorSupplier.get());
            }

            final T r = iter.next();

            if (iter.hasNext()) {
                throw new IllegalStateException("too many rows matching " + errorSupplier.get());
            }

            return r;
        }
    }
}
