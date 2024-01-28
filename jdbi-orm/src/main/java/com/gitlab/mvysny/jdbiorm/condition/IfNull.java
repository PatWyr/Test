package com.gitlab.mvysny.jdbiorm.condition;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * The IFNULL(arg1, arg2) expression. Not supported by all databases.
 * @param <V>
 */
public final class IfNull<V> implements Expression<V> {
    @NotNull
    private final Expression<? extends V> arg1;
    @NotNull
    private final Expression<? extends V> arg2;

    public IfNull(@NotNull Expression<? extends V> arg1, @NotNull Expression<? extends V> arg2) {
        this.arg1 = Objects.requireNonNull(arg1);
        this.arg2 = Objects.requireNonNull(arg2);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IfNull)) return false;
        IfNull<?> coalesce = (IfNull<?>) o;
        return Objects.equals(arg1, coalesce.arg1) && Objects.equals(arg2, coalesce.arg2);
    }

    @Override
    public int hashCode() {
        return Objects.hash(arg1, arg2);
    }

    @Override
    public String toString() {
        return "IFNULL(" + arg1 + ", " + arg2 + ")";
    }

    @NotNull
    public Expression<? extends V> getArg1() {
        return arg1;
    }

    @NotNull
    public Expression<? extends V> getArg2() {
        return arg2;
    }

    @Override
    public @NotNull ParametrizedSql toSql() {
        final ParametrizedSql sql1 = arg1.toSql();
        final ParametrizedSql sql2 = arg2.toSql();
        return ParametrizedSql.merge("IFNULL(" + sql1.getSql92() + ", " + sql2.getSql92() + ")", sql1.getSql92Parameters(), sql2.getSql92Parameters());
    }

    @Override
    public @Nullable Object calculate(@NotNull Object row) {
        final Object value = arg1.calculate(row);
        return value == null ? arg2.calculate(row) : value;
    }
}
