package com.gitlab.mvysny.jdbiorm.condition;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a native SQL where clause, for example
 * <code>age = 25 and name like :name</code>.
 * <p></p>
 * Don't forget to properly fill in the {@link #params}
 * map to contain values for all parameters. Make sure to avoid parameter name clashes
 * with other conditions which might bring their own parameters; generally avoid
 * parameter names produced by {@link ParametrizedSql#generateParameterName(Object)}.
 * <p></p>
 * Does not support in-memory filtering: {@link #test(Object)} will throw an exception.
 */
public final class NativeSQL implements Condition {
    /**
     * Expression which may go into the SQL WHERE clause, for example
     * <code>age = 25 and name like :name</code>. Avoid adding parameter values
     * here, place them into the {@link #params} map instead, to avoid SQL injection.
     */
    @NotNull
    private final String where;
    /**
     * Maps parameter names to values. For example if the {@link #where} clause refers
     * to a 'name' parameter via ':name', have an entry "name" mapped to e.g. "Foo".
     * Unmodifiable.
     */
    @NotNull
    private final Map<String, Object> params;

    /**
     * Creates the native SQL condition.
     * @param where Expression which may go into the SQL WHERE clause, for example
     *       <code>age = 25 and name like :name</code>. Avoid adding parameter values
     *       here, place them into the {@link #params} map instead, to avoid SQL injection.
     * @param params Maps parameter names to values. For example if the {@link #where} clause refers
     *      to a 'name' parameter via ':name', have an entry "name" mapped to e.g. "Foo". Unmodifiable.
     */
    public NativeSQL(@NotNull String where, @NotNull Map<String, Object> params) {
        this.where = Objects.requireNonNull(where);
        this.params = Map.copyOf(params); // defensive copy
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NativeSQL)) return false;
        NativeSQL nativeSQL = (NativeSQL) o;
        return Objects.equals(where, nativeSQL.where) && Objects.equals(params, nativeSQL.params);
    }

    @Override
    public int hashCode() {
        return Objects.hash(where, params);
    }

    @Override
    public String toString() {
        return '\'' + where + '\'' + params;
    }

    /**
     * Expression which may go into the SQL WHERE clause, for example
     * <code>age = 25 and name like :name</code>. Avoid adding parameter values
     * here, place them into the {@link #params} map instead, to avoid SQL injection.
     */
    @NotNull
    public String getWhere() {
        return where;
    }

    /**
     * Maps parameter names to values. For example if the {@link #where} clause refers
     * to a 'name' parameter via ':name', have an entry "name" mapped to e.g. "Foo".
     * Unmodifiable.
     */
    @NotNull
    public Map<String, Object> getParams() {
        return params;
    }

    @Override
    public @NotNull ParametrizedSql toSql() {
        return new ParametrizedSql(where, params);
    }

    /**
     * Does not support in-memory filtering and will throw the {@link UnsupportedOperationException}.
     * @throws UnsupportedOperationException always thrown.
     */
    @Override
    public boolean test(@NotNull Object row) {
        throw new UnsupportedOperationException("Does not support in-memory filtering");
    }
}
