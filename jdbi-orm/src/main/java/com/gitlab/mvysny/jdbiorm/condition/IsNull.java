package com.gitlab.mvysny.jdbiorm.condition;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * The <code>IS NULL</code> expression; not to be mistaken for the <code>ISNULL()</code> function.
 */
public final class IsNull implements Condition {
    @NotNull
    private final Expression<?> arg;

    public IsNull(@NotNull Expression<?> arg) {
        this.arg = Objects.requireNonNull(arg);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IsNull isTrue = (IsNull) o;
        return Objects.equals(arg, isTrue.arg);
    }

    @Override
    public int hashCode() {
        return Objects.hash(arg);
    }

    @Override
    public String toString() {
        return arg + " IS NULL";
    }

    public @NotNull Expression<?> getArg() {
        return arg;
    }

    @Override
    public @NotNull ParametrizedSql toSql() {
        final ParametrizedSql sql = arg.toSql();
        return new ParametrizedSql("(" + sql.getSql92() + ") IS NULL", sql.getSql92Parameters());
    }

    @Override
    public boolean test(@NotNull Object row) {
        return arg.calculate(row) == null;
    }
}
