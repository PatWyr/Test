package com.gitlab.mvysny.jdbiorm.condition;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * The <code>EQ</code> statement.
 */
public final class Eq implements Condition {
    @NotNull
    private final Expression<?> arg1;
    @NotNull
    private final Expression<?> arg2;

    public Eq(@NotNull Expression<?> arg1, @NotNull Expression<?> arg2) {
        this.arg1 = Objects.requireNonNull(arg1);
        this.arg2 = Objects.requireNonNull(arg2);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Eq eq = (Eq) o;
        return Objects.equals(arg1, eq.arg1) && Objects.equals(arg2, eq.arg2);
    }

    @Override
    public int hashCode() {
        return Objects.hash(arg1, arg2);
    }

    @Override
    public String toString() {
        return arg1 + " = " + arg2;
    }

    @NotNull
    public Expression<?> getArg1() {
        return arg1;
    }

    @NotNull
    public Expression<?> getArg2() {
        return arg2;
    }

    @Override
    public @NotNull ParametrizedSql toSql() {
        return ParametrizedSql.mergeWithOperator("=", arg1.toSql(), arg2.toSql());
    }

    @Override
    public boolean test(@NotNull Object row) {
        final Object value1 = arg1.calculate(row);
        if (value1 == null) {
            return false; // in SQL, null is not equal to anything
        }
        final Object value2 = arg2.calculate(row);
        if (value2 == null) {
            return false; // in SQL, null is not equal to anything
        }
        return value1.equals(value2);
    }
}
