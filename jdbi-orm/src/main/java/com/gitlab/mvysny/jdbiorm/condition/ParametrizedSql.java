package com.gitlab.mvysny.jdbiorm.condition;

import org.jdbi.v3.core.statement.SqlStatement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Contains a native SQL-92 query or just a query part (e.g. only the part after WHERE),
 * possibly referencing named parameters.
 * <p></p>
 * For example, you can create the following SQL WHERE part:
 * <code><pre>
 * new ParametrizedSql("PERSON.NAME = :name", "name", "John")
 * </pre></code>
 * You can write any kind of clause and obviously this is vulnerable to SQL injection attack
 * if you're not careful. It's better to use the {@link Expression} API instead -
 * the expression will ultimately be converted to an instance of this object but will
 * use {@link #generateParameterName(Object)} to avoid parameter name clashes
 * and will always pass in values via parameters.
 */
public final class ParametrizedSql implements Serializable {
    /**
     * For example `name = :name`; references database column names via
     * {@link com.gitlab.mvysny.jdbiorm.Property.DbName}. You can generate parameter names
     * using {@link #generateParameterName(Object)}.
     */
    @NotNull
    private final String sql92;

    /**
     * Maps parameter names mentioned in {@link #sql92} to their values. Unmodifiable.
     */
    @NotNull
    private final Map<String, Object> sql92Parameters;

    /**
     * Creates a SQL clause with additional parameters.
     * @param sql92 the SQL WHERE clause, not null.
     * @param sql92Parameters optional additional parameters.
     */
    public ParametrizedSql(@NotNull String sql92, @NotNull Map<String, Object> sql92Parameters) {
        this.sql92 = sql92;
        this.sql92Parameters = Collections.unmodifiableMap(sql92Parameters);
    }

    @NotNull
    public static ParametrizedSql merge(@NotNull String sql92, @NotNull Map<String, Object> sql92Parameters, @NotNull Map<String, Object> additionalSql92Parameters) {
        final HashMap<String, Object> params = new HashMap<>(sql92Parameters);
        params.putAll(additionalSql92Parameters);
        return new ParametrizedSql(sql92, params);
    }

    @NotNull
    public static ParametrizedSql mergeWithOperator(@NotNull String operator, @NotNull ParametrizedSql sql1, @NotNull ParametrizedSql sql2) {
        return ParametrizedSql.merge("(" + sql1.getSql92() + ") " + operator + " (" + sql2.getSql92() + ")", sql1.getSql92Parameters(), sql2.getSql92Parameters());
    }

    public ParametrizedSql(@NotNull String sql92) {
        this(sql92, Collections.emptyMap());
    }

    public ParametrizedSql(@NotNull String sql92, @NotNull String parameterName, @Nullable Object parameterValue) {
        this(sql92, Collections.singletonMap(parameterName, parameterValue));
    }

    /**
     * Generates a valid SQL parameter name, to be stored in {@link #getSql92Parameters()}. Avoids name clashes.
     * <p></p>
     * This is typically not needed when you type the WHERE clause yourself and you only have a handful of parameters, since
     * you can avoid the name clashes easily yourself; however this is useful when the SQL clause is generated from a bunch of
     * {@link Expression expressions}.
     * @param expression the reference to expression. The idea is that the expressions do not repeat in
     *                   one SQL clause, giving the possibility for us to generate the name using {@link System#identityHashCode(Object)}. This is
     *                   implementation detail and may change in the future.
     * @return a parameter name unique for this expression, not null.
     */
    @NotNull
    public static String generateParameterName(@NotNull Object expression) {
        return "p" + Integer.toString(System.identityHashCode(expression), 36);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ParametrizedSql that = (ParametrizedSql) o;
        return Objects.equals(sql92, that.sql92) && Objects.equals(sql92Parameters, that.sql92Parameters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sql92, sql92Parameters);
    }

    @Override
    public String toString() {
        return sql92 + ":" + sql92Parameters;
    }

    /**
     * For example `name = :name`; references database column names via
     * {@link com.gitlab.mvysny.jdbiorm.Property.DbName}. You can generate parameter names
     * using {@link #generateParameterName(Object)}.
     */
    public @NotNull String getSql92() {
        return sql92;
    }

    /**
     * Maps parameter names mentioned in {@link #sql92} to their values. Unmodifiable.
     */
    public @NotNull Map<String, Object> getSql92Parameters() {
        return sql92Parameters;
    }

    public void bindTo(@NotNull SqlStatement<?> query) {
        for (Map.Entry<String, Object> e : sql92Parameters.entrySet()) {
            query.bind(e.getKey(), e.getValue());
        }
    }

    @NotNull
    public static final ParametrizedSql MATCH_ALL = new ParametrizedSql("1=1");
}
