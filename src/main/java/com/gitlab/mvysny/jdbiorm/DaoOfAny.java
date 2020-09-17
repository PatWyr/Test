package com.gitlab.mvysny.jdbiorm;

import com.gitlab.mvysny.jdbiorm.quirks.Quirks;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.mapper.reflect.FieldMapper;
import org.jdbi.v3.core.result.ResultIterable;
import org.jdbi.v3.core.result.ResultIterator;
import org.jdbi.v3.core.statement.Binding;
import org.jdbi.v3.core.statement.Query;
import org.jdbi.v3.core.statement.Update;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.gitlab.mvysny.jdbiorm.JdbiOrm.jdbi;

/**
 * Sometimes you don't want a class to be an entity for some reason (e.g. when it doesn't have a primary key),
 * but still it's mapped to a table and you would want to have Dao support for that class.
 * Just let your class have a static field, for example:
 * <pre>
 * public class Log {
 *   public static final DaoOfAny&lt;Log&gt; dao = new DaoOfAny&lt&gt;(Log.class);
 * }
 * </pre>
 * You can now use `Log.dao.findAll()`, `Log.dao.count()` and other nice methods.
 * <p></p>
 * Use {@link Table} to change the database table name (by default it's the class
 * simple name); use {@link org.jdbi.v3.core.mapper.reflect.ColumnName} to set
 * the database column name.
 * @param <T> the type of the class provided by this Dao
 */
public class DaoOfAny<T> implements Serializable {
    @NotNull
    public final Class<T> entityClass;  // public because of vok-orm
    @NotNull
    protected final EntityMeta<T> meta;   // not public, to not to pollute the API
    @NotNull
    protected final Helper<T> helper;  // not public, to not to pollute the API

    public DaoOfAny(@NotNull Class<T> entityClass) {
        this.entityClass = Objects.requireNonNull(entityClass, "entityClass");
        meta = new EntityMeta<>(entityClass);
        helper = new Helper<>(entityClass);
    }

    /**
     * Returns JDBI Row Mapper which maps JDBC ResultSet to Java object. By default
     * returns FieldMapper, and it is recommended to use FieldMapper unless you need
     * to perform some hard-core JDBI mapper tweaking.
     * @return FieldMapper.of(entityClass) by default. Not null.
     */
    @NotNull
    public RowMapper<T> getRowMapper() {  // public because of vok-orm
        return FieldMapper.of(entityClass);
    }

    /**
     * Finds all rows in given table. Fails if there is no table in the database with the
     * name of {@link EntityMeta#getDatabaseTableName()}. The list is eager
     * and thus it's useful for small-ish tables only.
     */
    @NotNull
    public List<T> findAll() {
        return jdbi().withHandle(handle -> handle.createQuery("select <FIELDS> from <TABLE>")
                .define("FIELDS", String.join(", ", meta.getPersistedFieldDbNames()))
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
        if (limit != null && limit == 0L) {
            return new ArrayList<>();
        }
        final StringBuilder sql = new StringBuilder("select <FIELDS> from <TABLE>");
        checkOffsetLimit(offset, limit);
        return jdbi().withHandle(handle -> {
                    appendOffsetLimit(sql, handle, offset, limit);
                    return handle.createQuery(sql.toString())
                            .define("FIELDS", String.join(", ", meta.getPersistedFieldDbNames()))
                            .define("TABLE", meta.getDatabaseTableName())
                            .map(getRowMapper())
                            .list();
                }
        );
    }

    private static void checkOffsetLimit(@Nullable Long offset, @Nullable Long limit) {
        if (offset != null && offset < 0) {
            throw new IllegalArgumentException("Parameter offset: invalid value " + offset + ": must be 0 or greater");
        }
        if (limit != null && limit < 0) {
            throw new IllegalArgumentException("Parameter limit: invalid value " + limit + ": must be 0 or greater");
        }
    }

    protected void appendOffsetLimit(@NotNull StringBuilder sb, @NotNull Handle handle, @Nullable final Long offset, @Nullable final Long limit) {
        if (offset == null && limit == null) {
            return;
        }
        final Quirks quirks = Quirks.from(handle);
        if (quirks.offsetLimitRequiresOrderBy() != null) {
            sb.append(" ").append(quirks.offsetLimitRequiresOrderBy());
        }
        sb.append(" ").append(quirks.offsetLimit(offset, limit));
    }

    /**
     * Finds all matching rows in given table. Fails if there is no table in the database with the
     * name of {@link EntityMeta#getDatabaseTableName()}. If both offset and limit
     * are specified, then the LIMIT and OFFSET sql paging is used.
     * @param where the where clause, e.g. {@code name = :name}. Careful: this goes into the SQL as-is - could be misused for SQL injection!
     * @param offset start from this row. If not null, must be 0 or greater.
     * @param limit return this count of row at most. If not null, must be 0 or greater.
     * @param queryConsumer allows you to set parameter values etc, for example {@code q -> q.bind("customerid", customerId")}.
     */
    @NotNull
    public List<T> findAllBy(@NotNull String where, @Nullable final Long offset, @Nullable final Long limit, @NotNull Consumer<Query> queryConsumer) {
        Objects.requireNonNull(where, "where");
        Objects.requireNonNull(queryConsumer, "queryConsumer");
        if (limit != null && limit == 0L) {
            return new ArrayList<>();
        }
        final StringBuilder sql = new StringBuilder("select <FIELDS> from <TABLE> where <WHERE>");
        checkOffsetLimit(offset, limit);
        return jdbi().withHandle(handle -> {
                    appendOffsetLimit(sql, handle, offset, limit);
                    final Query query = handle.createQuery(sql.toString())
                            .define("FIELDS", String.join(", ", meta.getPersistedFieldDbNames()))
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
     * Finds all matching rows in given table. Fails if there is no table in the database with the
     * name of {@link EntityMeta#getDatabaseTableName()}.
     * @param where the where clause, e.g. {@code name = :name}. Careful: this goes into the SQL as-is - could be misused for SQL injection!
     * @param queryConsumer allows you to set parameter values etc, for example {@code q -> q.bind("customerid", customerId")}.
     */
    @NotNull
    public List<T> findAllBy(@NotNull String where, @NotNull Consumer<Query> queryConsumer) {
        return findAllBy(where, null, null, queryConsumer);
    }

    /**
     * Retrieves single entity matching given {@code where} clause.
     * Returns null if there is no such entity; fails if there are two or more entities matching the criteria.
     * <p></p>
     * Example:
     * <pre>
     * Person.dao.findOneBy("name = :name", q -> q.bind("name", "Albedo"))
     * </pre>
     * <p>
     * This function returns null if there is no item matching. Use {@link #getOneBy(String, Consumer)}
     * if you wish to fail with an exception in case that the entity does not exist.
     *
     * @param where the where clause, e.g. {@code name = :name}. Careful: this goes into the SQL as-is - could be misused for SQL injection!
     * @param queryConsumer allows you to set parameter values etc, for example {@code q -> q.bind("customerid", customerId")}.
     * @throws IllegalStateException if there are two or more matching entities.
     */
    @Nullable
    public T findOneBy(@NotNull String where, @NotNull Consumer<Query> queryConsumer) {
        Objects.requireNonNull(where, "where");
        Objects.requireNonNull(queryConsumer, "queryConsumer");
        return jdbi().withHandle(handle -> {
            String sql = "select <FIELDS> from <TABLE> where <WHERE>";
            final Quirks quirks = Quirks.from(handle);
            if (quirks.offsetLimitRequiresOrderBy() != null) {
                sql += " " + quirks.offsetLimitRequiresOrderBy();
            }
            sql += quirks.offsetLimit(null, 2L);
            final Query query = handle.createQuery(sql)
                    .define("FIELDS", String.join(", ", meta.getPersistedFieldDbNames()))
                    .define("TABLE", meta.getDatabaseTableName())
                    .define("WHERE", where);
            queryConsumer.accept(query);
            final ResultIterable<T> iterable = query.map(getRowMapper());
            return helper.findOneFromIterable(iterable, binding -> helper.formatQuery(where, binding));
        });
    }

    /**
     * Retrieves the single entity from this table. Useful for config table which hosts one row only.
     * Returns null if there is no such entity; fails if there are two or more entities matching the criteria.
     * <p></p>
     * Example:
     * <pre>
     * ConfigTable.dao.findOne()
     * </pre>
     * <p>
     * This function returns null if there is no item matching. Use {@link #getOneBy(String, Consumer)}
     * if you wish to return `null` in case that the entity does not exist.
     *
     * @throws IllegalStateException if there are two or more rows.
     */
    @Nullable
    public T findOne() {
        return jdbi().withHandle(handle -> {
            String sql = "select <FIELDS> from <TABLE>";
            final Quirks quirks = Quirks.from(handle);
            if (quirks.offsetLimitRequiresOrderBy() != null) {
                sql += " " + quirks.offsetLimitRequiresOrderBy();
            }
            sql += quirks.offsetLimit(null, 2L);
            final ResultIterable<T> iterable = handle.createQuery(sql)
                    .define("FIELDS", String.join(", ", meta.getPersistedFieldDbNames()))
                    .define("TABLE", meta.getDatabaseTableName())
                    .map(getRowMapper());
            return helper.findOneFromIterable(iterable, binding -> helper.formatQuery("", binding));
        });
    }

    /**
     * Retrieves the single entity from this table. Useful for config table which hosts one row only.
     * Returns null if there is no such entity; fails if there are two or more entities matching the criteria.
     * <p></p>
     * Example:
     * <pre>
     * ConfigTable.dao.findOne()
     * </pre>
     * <p>
     * This function fails if there is no item matching. Use {@link #findOne}
     * if you wish to return `null` in case that the entity does not exist.
     *
     * @throws IllegalStateException if the table is empty, or if there are two or more rows.
     */
    @Nullable
    public T getOne() {
        return jdbi().withHandle(handle -> {
            String sql = "select <FIELDS> from <TABLE>";
            final Quirks quirks = Quirks.from(handle);
            if (quirks.offsetLimitRequiresOrderBy() != null) {
                sql += " " + quirks.offsetLimitRequiresOrderBy();
            }
            sql += quirks.offsetLimit(null, 2L);
            final ResultIterable<T> iterable = handle.createQuery(sql)
                    .define("FIELDS", String.join(", ", meta.getPersistedFieldDbNames()))
                    .define("TABLE", meta.getDatabaseTableName())
                    .map(getRowMapper());
            return helper.getOneFromIterable(iterable, binding -> helper.formatQuery("", binding));
        });
    }

    /**
     * Retrieves single random entity. Returns null if there is no such entity.
     */
    @Nullable
    public T findFirst() {
        final List<T> first = findAll(0L, 1L);
        return first.isEmpty() ? null : first.get(0);
    }

    /**
     * Retrieves first entity from the list of entities matching given {@code where} clause.
     * Returns null if there is no such entity.
     * <p></p>
     * Example:
     * <pre>
     * Person.dao.findFirstBy("name = :name", q -> q.bind("name", "Albedo"))
     * </pre>
     * <p>
     * This function returns null if there is no item matching.
     *
     * @param where the where clause, e.g. {@code name = :name}. Careful: this goes into the SQL as-is - could be misused for SQL injection!
     * @param queryConsumer allows you to set parameter values etc, for example {@code q -> q.bind("customerid", customerId")}.
     */
    @Nullable
    public T findFirstBy(@NotNull String where, @NotNull Consumer<Query> queryConsumer) {
        final List<T> first = findAllBy(where, 0L, 1L, queryConsumer);
        return first.isEmpty() ? null : first.get(0);
    }

    /**
     * Retrieves single entity matching given {@code where} clause.
     * Fails if there is no such entity, or if there are two or more entities matching the criteria.
     * <p></p>
     * Example:
     * <pre>
     * Person.dao.getOneBy("name = :name", q -> q.bind("name", "Albedo"))
     * </pre>
     * <p>
     * This function fails if there is no such entity or there are 2 or more. Use [findSpecificBy] if you wish to return `null` in case that
     * the entity does not exist.
     *
     * @param where the where clause, e.g. {@code name = :name}. Careful: this goes into the SQL as-is - could be misused for SQL injection!
     * @param queryConsumer allows you to set parameter values etc, for example {@code q -> q.bind("customerid", customerId")}.
     * @throws IllegalStateException if there is no entity matching given criteria, or if there are two or more matching entities.
     */
    @NotNull
    public T getOneBy(@NotNull String where, Consumer<Query> queryConsumer) {
        Objects.requireNonNull(where, "where");
        Objects.requireNonNull(queryConsumer, "queryConsumer");
        return jdbi().withHandle(handle -> {
            String sql = "select <FIELDS> from <TABLE> where <WHERE>";
            final Quirks quirks = Quirks.from(handle);
            if (quirks.offsetLimitRequiresOrderBy() != null) {
                sql += " " + quirks.offsetLimitRequiresOrderBy();
            }
            sql += quirks.offsetLimit(null, 2L);
            final Query query = handle.createQuery(sql)
                            .define("FIELDS", String.join(", ", meta.getPersistedFieldDbNames()))
                            .define("TABLE", meta.getDatabaseTableName())
                            .define("WHERE", where);
                    queryConsumer.accept(query);
                    final ResultIterable<T> result = query.map(getRowMapper());
                    return helper.getOneFromIterable(result, binding -> helper.formatQuery(where, binding));
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
     * @param where the where clause, e.g. {@code name = :name}. Careful: this goes into the SQL as-is - could be misused for SQL injection!
     * @param queryConsumer allows you to set parameter values etc, for example {@code q -> q.bind("customerid", customerId")}.
     */
    public long countBy(@NotNull String where, @NotNull Consumer<Query> queryConsumer) {
        Objects.requireNonNull(where, "where");
        Objects.requireNonNull(queryConsumer, "queryConsumer");
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
     * @param where the where clause, e.g. {@code name = :name}. Careful: this goes into the SQL as-is - could be misused for SQL injection!
     * @param queryConsumer allows you to set parameter values etc, for example {@code q -> q.bind("customerid", customerId")}.
     */
    public boolean existsBy(@NotNull String where, @NotNull Consumer<Query> queryConsumer) {
        Objects.requireNonNull(where, "where");
        Objects.requireNonNull(queryConsumer, "queryConsumer");
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
     * @param where the where clause, e.g. {@code name = :name}. Careful: this goes into the SQL as-is - could be misused for SQL injection!
     * @param updateConsumer allows you to set parameter values etc, for example {@code q -> q.bind("customerid", customerId")}.
     */
    public void deleteBy(@NotNull String where, @NotNull Consumer<Update> updateConsumer) {
        Objects.requireNonNull(where, "where");
        Objects.requireNonNull(updateConsumer, "updateConsumer");
        jdbi().withHandle(handle -> {
            final Update update = handle.createUpdate("delete from <TABLE> where <WHERE>")
                    .define("TABLE", meta.getDatabaseTableName())
                    .define("WHERE", where);
            updateConsumer.accept(update);
            return update.execute();
        });
    }

    /**
     * Helper functions which should not be auto-completed in {@link DaoOfAny} are placed here.
     * @param <T>
     */
    public static final class Helper<T> implements Serializable {
        @NotNull
        public final Class<T> entityClass;

        public Helper(@NotNull Class<T> entityClass) {
            this.entityClass = Objects.requireNonNull(entityClass);
        }

        /**
         * Provides detailed debug info which is helpful when the query fails.
         * @param sql the SQL
         * @param binding the binding
         * @return the entity name, the SQL and values of all the parameters
         */
        @NotNull
        public String formatQuery(@NotNull String sql, @NotNull Binding binding) {
            Objects.requireNonNull(sql, "sql");
            return entityClass.getSimpleName() + ": '" + sql + "'" + binding;
        }

        /**
         * Returns the only row in the result set, if any. Returns null if zero
         * rows are returned, or if the row itself is {@code null}.
         *
         * @param errorSupplier invoked in case of error; should list table name, the {@code where} clause
         *                      and parameters. Prepended by 'too many items matching '. Simply use
         *                      {@link #formatQuery(String, Binding)}.
         * @return the only row in the result set, if any.
         * @throws IllegalStateException if the result set contains multiple rows
         */
        @Nullable
        public T findOneFromIterable(@NotNull ResultIterable<T> iterable, @NotNull Function<Binding, String> errorSupplier) {
            Objects.requireNonNull(iterable, "iterable");
            Objects.requireNonNull(errorSupplier, "errorSupplier");
            try (ResultIterator<T> iter = iterable.iterator()) {
                if (!iter.hasNext()) {
                    return null;
                }

                final T r = iter.next();

                if (iter.hasNext()) {
                    throw new IllegalStateException("too many rows matching " + errorSupplier.apply(iter.getContext().getBinding()));
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
         *                      {@link #formatQuery(String, Binding)} .
         * @return the only row in the result set.
         * @throws IllegalStateException if the result set contains zero or multiple rows
         */
        @NotNull
        public T getOneFromIterable(ResultIterable<T> iterable, @NotNull Function<Binding, String> errorSupplier) {
            try (ResultIterator<T> iter = iterable.iterator()) {
                if (!iter.hasNext()) {
                    throw new IllegalStateException("no row matching " + errorSupplier.apply(iter.getContext().getBinding()));
                }

                final T r = iter.next();

                if (iter.hasNext()) {
                    throw new IllegalStateException("too many rows matching " + errorSupplier.apply(iter.getContext().getBinding()));
                }

                return r;
            }
        }
    }
}
