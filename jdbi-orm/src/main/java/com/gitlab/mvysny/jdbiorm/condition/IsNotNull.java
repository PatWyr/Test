package com.gitlab.mvysny.jdbiorm.condition;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class IsNotNull implements Condition {
    @NotNull
    private final Expression<?> arg;

    public IsNotNull(@NotNull Expression<?> arg) {
        this.arg = Objects.requireNonNull(arg);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IsNotNull isTrue = (IsNotNull) o;
        return Objects.equals(arg, isTrue.arg);
    }

    @Override
    public int hashCode() {
        return Objects.hash(arg);
    }

    @Override
    public String toString() {
        return arg + " IS NOT NULL";
    }

    public @NotNull Expression<?> getArg() {
        return arg;
    }

    @Override
    public @NotNull ParametrizedSql toSql() {
        final ParametrizedSql sql = arg.toSql();
        return new ParametrizedSql("(" + sql.getSql92() + ") IS NOT NULL", sql.getSql92Parameters());
    }
}
