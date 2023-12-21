package com.gitlab.mvysny.jdbiorm.condition;

import org.jdbi.v3.core.statement.Query;
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
 */
public final class ParametrizedSql implements Serializable {
    /**
     * For example `name = :name`; references database column names via
     * {@link com.gitlab.mvysny.jdbiorm.Property.DbName}. You can generate parameter names
     * using {@link #generateParameterName(Expression)}.
     */
    @NotNull
    private final String sql92;

    /**
     * Maps parameter names mentioned in {@link #sql92} to their values. Unmodifiable.
     */
    @NotNull
    private final Map<String, Object> sql92Parameters;

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

    @NotNull
    public static String generateParameterName(@NotNull Expression<?> expression) {
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
     * using {@link #generateParameterName(Expression)}.
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
}
