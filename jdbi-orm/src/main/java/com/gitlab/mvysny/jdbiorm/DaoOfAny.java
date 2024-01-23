package com.gitlab.mvysny.jdbiorm;

import com.gitlab.mvysny.jdbiorm.condition.Condition;
import com.gitlab.mvysny.jdbiorm.condition.ParametrizedSql;
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
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.gitlab.mvysny.jdbiorm.JdbiOrm.jdbi;

/**
 * Sometimes you don't want a class to be an entity for some reason
 * (e.g. you don't want to edit the rows of a table, or the table doesn't have a primary key),
 * but still it's mapped to a table and you would want to have Dao support for that class.
 * Just let your class have a static field, for example:
 * <pre>
 * public class Log {
 *   public static final DaoOfAny&lt;Log&gt; dao = new DaoOfAny&lt;&gt;(Log.class);
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
    protected final EntityMeta<T> meta;   // not public, to not pollute the API
    @NotNull
    protected final Helper<T> helper;  // not public, to not pollute the API

    public DaoOfAny(@NotNull Class<T> entityClass) {
        this.entityClass = Objects.requireNonNull(entityClass, "entityClass");
        meta = EntityMeta.of(entityClass);
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
     * name of {@link EntityMeta#getDatabaseTableName()}. The list is eager,
     * therefore it's useful for small-ish tables only.
     */
    @NotNull
    public List<T> findAll() {
        return findAll((String) null, null, null);
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
        return findAll((String) null, offset, limit);
    }

    /**
     * Finds all rows in given table. Fails if there is no table in the database with the
     * name of {@link EntityMeta#getDatabaseTableName()}. If both offset and limit
     * are specified, then the LIMIT and OFFSET sql paging is used.
     * @param orderBy if not null, this is passed in as the ORDER BY clause, e.g. {@code surname ASC, name ASC}. Careful: this goes into the SQL as-is - could be misused for SQL injection!
     * @param offset start from this row. If not null, must be 0 or greater.
     * @param limit return this count of row at most. If not null, must be 0 or greater.
     */
    @NotNull
    public List<T> findAll(@Nullable String orderBy, @Nullable final Long offset, @Nullable final Long limit) {
        if (limit != null && limit == 0L) {
            return new ArrayList<>();
        }
        final StringBuilder sql = new StringBuilder("select <FIELDS> from <TABLE>");
        checkOffsetLimit(offset, limit);
        return jdbi().withHandle(handle -> {
                    if (orderBy != null) {
                        sql.append(" order by ").append(orderBy);
                    }
                    // H2 requires ORDER BY after LIMIT+OFFSET clauses.
                    appendOffsetLimit(sql, handle, offset, limit, orderBy != null);
                    return handle.createQuery(sql.toString())
                            .define("FIELDS", meta.getPersistedFieldDbNames().stream().map(Property.DbName::getQualifiedName).collect(Collectors.joining(", ")))
                            .define("TABLE", meta.getDatabaseTableName())
                            .map(getRowMapper())
                            .list();
                }
        );
    }

    /**
     * Finds all rows in given table. Fails if there is no table in the database with the
     * name of {@link EntityMeta#getDatabaseTableName()}. If both offset and limit
     * are specified, then the LIMIT and OFFSET sql paging is used.
     * @param orderBy if not empty, this is passed in as the ORDER BY clause. May be empty, in such case no ordering is applied.
     * @param offset start from this row. If not null, must be 0 or greater.
     * @param limit return this count of row at most. If not null, must be 0 or greater.
     */
    @NotNull
    public List<T> findAll(@NotNull List<OrderBy> orderBy, @Nullable final Long offset, @Nullable final Long limit) {
        if (limit != null && limit == 0L) {
            return new ArrayList<>();
        }
        final String order = toSqlOrderClause(orderBy);
        return findAll(order, offset, limit);
    }

    @Nullable
    private String toSqlOrderClause(@NotNull List<OrderBy> orderBy) {
        final String order = orderBy.isEmpty() ? null : orderBy.stream()
                .map(it -> it.getProperty().getDbName().getQualifiedName() + " " + it.getOrder())
                .collect(Collectors.joining(", "));
        return order;
    }

    protected static void checkOffsetLimit(@Nullable Long offset, @Nullable Long limit) {
        if (offset != null && offset < 0) {
            throw new IllegalArgumentException("Parameter offset: invalid value " + offset + ": must be 0 or greater");
        }
        if (limit != null && limit < 0) {
            throw new IllegalArgumentException("Parameter limit: invalid value " + limit + ": must be 0 or greater");
        }
    }

    @NotNull
    protected String getOffsetLimit(@NotNull Handle handle, @Nullable final Long offset, @Nullable final Long limit, boolean sqlHasOrderBy) {
        final StringBuilder sb = new StringBuilder();
        appendOffsetLimit(sb, handle, offset, limit, sqlHasOrderBy);
        return sb.toString().trim();
    }

    protected void appendOffsetLimit(@NotNull StringBuilder sql, @NotNull Handle handle, @Nullable final Long offset, @Nullable final Long limit, boolean sqlHasOrderBy) {
        if (offset == null && limit == null) {
            return;
        }
        final Quirks quirks = Quirks.from(handle);
        if (quirks.offsetLimitRequiresOrderBy() != null && !sqlHasOrderBy) {
            sql.append(" ").append(quirks.offsetLimitRequiresOrderBy());
        }
        sql.append(" ").append(quirks.offsetLimit(offset, limit));
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
        return findAllBy(where, null, offset, limit, queryConsumer);
    }

    /**
     * Finds all matching rows in given table. Fails if there is no table in the database with the
     * name of {@link EntityMeta#getDatabaseTableName()}. If both offset and limit
     * are specified, then the LIMIT and OFFSET sql paging is used.
     * @param where the where condition. If null, all rows are matched.
     * @param offset start from this row. If not null, must be 0 or greater.
     * @param limit return this count of row at most. If not null, must be 0 or greater.
     */
    @NotNull
    public List<T> findAllBy(@Nullable Condition where, @Nullable final Long offset, @Nullable final Long limit) {
        return findAllBy(where, Collections.emptyList(), offset, limit);
    }

    /**
     * Finds all matching rows in given table. Fails if there is no table in the database with the
     * name of {@link EntityMeta#getDatabaseTableName()}. If both offset and limit
     * are specified, then the LIMIT and OFFSET sql paging is used.
     * @param where the where condition. If null, all rows are matched.
     * @param orderBy if not null, this is passed in as the ORDER BY clause. May be empty, in such case no ordering is applied.
     * @param offset start from this row. If not null, must be 0 or greater.
     * @param limit return this count of row at most. If not null, must be 0 or greater.
     */
    @NotNull
    public List<T> findAllBy(@Nullable Condition where, @NotNull List<OrderBy> orderBy, @Nullable final Long offset, @Nullable final Long limit) {
        if (where == null || where == Condition.NO_CONDITION) {
            return findAll(orderBy, offset, limit);
        }
        final ParametrizedSql sql = where.toSql();
        final String order = toSqlOrderClause(orderBy);
        return findAllBy(sql.getSql92(), order, offset, limit, sql::bindTo);
    }

    /**
     * Finds all matching rows in given table. Fails if there is no table in the database with the
     * name of {@link EntityMeta#getDatabaseTableName()}.
     * @param where the where condition. If null, all rows are matched.
     * @param orderBy if not null, this is passed in as the ORDER BY clause. May be empty, in such case no ordering is applied.
     */
    @NotNull
    public List<T> findAllBy(@Nullable Condition where, @NotNull List<OrderBy> orderBy) {
        return findAllBy(where, orderBy, null, null);
    }

    /**
     * Finds all matching rows in given table. Fails if there is no table in the database with the
     * name of {@link EntityMeta#getDatabaseTableName()}. If both offset and limit
     * are specified, then the LIMIT and OFFSET sql paging is used.
     * @param where the where condition. If null, all rows are matched.
     */
    @NotNull
    public List<T> findAllBy(@Nullable Condition where) {
        return findAllBy(where, null, null);
    }

    /**
     * Finds all matching rows in given table. Fails if there is no table in the database with the
     * name of {@link EntityMeta#getDatabaseTableName()}. If both offset and limit
     * are specified, then the LIMIT and OFFSET sql paging is used.
     * @param where the where clause, e.g. {@code name = :name}. Careful: this goes into the SQL as-is - could be misused for SQL injection!
     * @param orderBy if not null, this is passed in as the ORDER BY clause, e.g. {@code surname ASC, name ASC}. Careful: this goes into the SQL as-is - could be misused for SQL injection!
     * @param offset start from this row. If not null, must be 0 or greater.
     * @param limit return this count of row at most. If not null, must be 0 or greater.
     * @param queryConsumer allows you to set parameter values etc, for example {@code q -> q.bind("customerid", customerId")}.
     */
    @NotNull
    public List<T> findAllBy(@NotNull String where, @Nullable String orderBy,
                             @Nullable final Long offset, @Nullable final Long limit,
                             @NotNull Consumer<Query> queryConsumer) {
        Objects.requireNonNull(where, "where");
        Objects.requireNonNull(queryConsumer, "queryConsumer");
        if (limit != null && limit == 0L) {
            return new ArrayList<>();
        }
        final StringBuilder sql = new StringBuilder("select <FIELDS> from <TABLE> where <WHERE>");
        if (orderBy != null) {
            sql.append(" order by ").append(orderBy);
        }
        checkOffsetLimit(offset, limit);
        return jdbi().withHandle(handle -> {
                    appendOffsetLimit(sql, handle, offset, limit, orderBy != null);
                    final Query query = handle.createQuery(sql.toString())
                            .define("FIELDS", meta.getPersistedFieldDbNames().stream().map(Property.DbName::getQualifiedName).collect(Collectors.joining(", ")))
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
        return findAllBy(where, null, queryConsumer);
    }

    /**
     * Finds all matching rows in given table. Fails if there is no table in the database with the
     * name of {@link EntityMeta#getDatabaseTableName()}.
     * @param where the where clause, e.g. {@code name = :name}. Careful: this goes into the SQL as-is - could be misused for SQL injection!
     * @param orderBy if not null, this is passed in as the ORDER BY clause, e.g. {@code surname ASC, name ASC}. Careful: this goes into the SQL as-is - could be misused for SQL injection!
     * @param queryConsumer allows you to set parameter values etc, for example {@code q -> q.bind("customerid", customerId")}.
     */
    @NotNull
    public List<T> findAllBy(@NotNull String where, @Nullable String orderBy, @NotNull Consumer<Query> queryConsumer) {
        return findAllBy(where, orderBy, null, null, queryConsumer);
    }

    /**
     * Retrieves single entity matching given {@code where} clause.
     * Returns null if there is no such entity; fails if there are two or more entities matching the criteria.
     * <p></p>
     * Example:
     * <pre>
     * Person.dao.findSingleBy("name = :name", q -&gt; q.bind("name", "Albedo"))
     * </pre>
     * <p>
     * This function returns null if there is no item matching. Use {@link #singleBy(String, Consumer)}
     * if you wish to fail with an exception in case that the entity does not exist.
     *
     * @param where the where clause, e.g. {@code name = :name}. Careful: this goes into the SQL as-is - could be misused for SQL injection!
     * @param queryConsumer allows you to set parameter values etc, for example {@code q -> q.bind("customerid", customerId")}.
     * @throws IllegalStateException if there are two or more matching entities.
     */
    @Nullable
    public T findSingleBy(@Nullable String where, @NotNull Consumer<Query> queryConsumer) {
        return findSingleBy(where, false, queryConsumer);
    }

    @Nullable
    public T findSingleBy(@Nullable Condition where) {
        if (where == null || where == Condition.NO_CONDITION) {
            return findSingle();
        }
        final ParametrizedSql sql = where.toSql();
        return findSingleBy(sql.getSql92(), sql::bindTo);
    }

    @Nullable
    private T findSingleBy(@Nullable String where, boolean failOnNoResult, @NotNull Consumer<Query> queryConsumer) {
        Objects.requireNonNull(queryConsumer, "queryConsumer");
        return jdbi().withHandle(handle -> {
            String sql = "select <FIELDS> from <TABLE>";
            if (where != null) {
                sql += " where <WHERE>";
            }
            final Quirks quirks = Quirks.from(handle);
            if (quirks.offsetLimitRequiresOrderBy() != null) {
                sql += " " + quirks.offsetLimitRequiresOrderBy();
            }
            sql += quirks.offsetLimit(null, 2L);
            final String sqlFinal = sql;
            final Query query = handle.createQuery(sqlFinal)
                    .define("FIELDS", meta.getPersistedFieldDbNames().stream().map(Property.DbName::getQualifiedName).collect(Collectors.joining(", ")))
                    .define("TABLE", meta.getDatabaseTableName());
            if (where != null) {
                query.define("WHERE", where);
            }
            queryConsumer.accept(query);
            final ResultIterable<T> iterable = query.map(getRowMapper());
            return helper.findSingleFromIterable(iterable, failOnNoResult, binding -> helper.formatQuery(where == null ? "" : where, binding));
        });
    }

    /**
     * Retrieves the single entity from this table. Useful for example for config table which hosts one row only.
     * Returns null if there is no such entity; fails if there are two or more entities matching the criteria.
     * <p></p>
     * Example:
     * <pre>
     * ConfigTable.dao.findSingle()
     * </pre>
     * <p>
     * This function returns null if there is no item matching. Use {@link #single()}
     * if you wish to throw an exception in case that the entity does not exist.
     *
     * @throws IllegalStateException if there are two or more rows.
     */
    @Nullable
    public T findSingle() {
        return findSingleBy(null, q -> {});
    }

    /**
     * Retrieves the single entity from this table. Useful for example for config table which hosts one row only.
     * Fails if there is no entity, or if there are two or more entities matching the criteria.
     * <p></p>
     * Example:
     * <pre>
     * ConfigTable.dao.single()
     * </pre>
     * <p>
     * This function fails if there is no item matching. Use {@link #findSingle}
     * if you wish to return `null` in case that the entity does not exist.
     *
     * @throws IllegalStateException if the table is empty, or if there are two or more rows.
     */
    @NotNull
    public T single() {
        return singleBy(null, q -> {});
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
     * Person.dao.findFirstBy("name = :name", q -&gt; q.bind("name", "Albedo"))
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

    @Nullable
    public T findFirstBy(@Nullable Condition where) {
        if (where == null || where == Condition.NO_CONDITION) {
            return findFirst();
        }
        final ParametrizedSql sql = where.toSql();
        return findFirstBy(sql.getSql92(), sql::bindTo);
    }

    /**
     * Retrieves single entity matching given {@code where} clause.
     * Fails if there is no such entity, or if there are two or more entities matching the criteria.
     * <p></p>
     * Example:
     * <pre>
     * Person.dao.singleBy("name = :name", q -&gt; q.bind("name", "Albedo"))
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
    public T singleBy(@Nullable String where, Consumer<Query> queryConsumer) {
        return Objects.requireNonNull(findSingleBy(where, true, queryConsumer));
    }

    @NotNull
    public T singleBy(@Nullable Condition where) {
        if (where == null || where == Condition.NO_CONDITION) {
            return single();
        }
        final ParametrizedSql sql = where.toSql();
        return singleBy(sql.getSql92(), sql::bindTo);
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
     * Counts all matching rows in this table.
     * @param condition the where condition. If null, all rows are matched.
     */
    public long countBy(@Nullable Condition condition) {
        if (condition == null || condition == Condition.NO_CONDITION) {
            return count();
        }
        final ParametrizedSql sql = condition.toSql();
        return countBy(sql.getSql92(), sql::bindTo);
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
     * Checks whether there exists any row in this table.
     * @param condition the where condition. If null, all rows are matched.
     */
    public boolean existsBy(@Nullable Condition condition) {
        if (condition == null || condition == Condition.NO_CONDITION) {
            return existsAny();
        }
        final ParametrizedSql sql = condition.toSql();
        return existsBy(sql.getSql92(), sql::bindTo);
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
     * Deletes rows matching given where clause.
     * @param condition the where condition. If null, all rows are matched.
     */
    public void deleteBy(@Nullable Condition condition) {
        if (condition == null || condition == Condition.NO_CONDITION) {
            deleteAll();
        } else {
            final ParametrizedSql sql = condition.toSql();
            deleteBy(sql.getSql92(), sql::bindTo);
        }
    }

    /**
     * Helper functions for {@link DaoOfAny}.
     * <p></p>
     * All functions which would only pollute IDE auto-completion are placed here.
     * These functions are generally not intended to be invoked by the user of this library.
     * @param <T> the entity type
     */
    public static final class Helper<T> implements Serializable {
        @NotNull
        public final Class<T> entityClass;

        public Helper(@NotNull Class<T> entityClass) {
            this.entityClass = Objects.requireNonNull(entityClass);
        }

        /**
         * Provides detailed debug info which is helpful when the query fails.
         * @param sql the SQL, may be the entire "SELECT * FROM ..." clause, or just the WHERE clause. Anything that's valuable to the programmer for debugging.
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
        public T findSingleFromIterable(@NotNull ResultIterable<T> iterable, boolean failOnNoResult, @NotNull Function<Binding, String> errorSupplier) {
            Objects.requireNonNull(iterable, "iterable");
            Objects.requireNonNull(errorSupplier, "errorSupplier");
            try (ResultIterator<T> iter = iterable.iterator()) {
                if (!iter.hasNext()) {
                    if (failOnNoResult) {
                        throw new IllegalStateException("no row matching " + errorSupplier.apply(iter.getContext().getBinding()));
                    }
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
         * Returns the one and only row in the result set. Never returns {@code null}.
         *
         * @param errorSupplier invoked in case of error; should list table name, the {@code where} clause
         *                      and parameters. Prepended by 'too many items matching '. Simply use
         *                      {@link #formatQuery(String, Binding)} .
         * @return the only row in the result set.
         * @throws IllegalStateException if the result set contains zero or multiple rows
         */
        @NotNull
        @Deprecated
        public T getSingleFromIterable(ResultIterable<T> iterable, @NotNull Function<Binding, String> errorSupplier) {
            return findSingleFromIterable(iterable, true, errorSupplier);
        }
    }
}
