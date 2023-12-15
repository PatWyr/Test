package com.gitlab.mvysny.jdbiorm.condition;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class Lower<V> implements Expression<V> {
    @NotNull
    private final Expression<V> arg;

    public Lower(@NotNull Expression<V> arg) {
        this.arg = arg;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Lower<?> lower = (Lower<?>) o;
        return Objects.equals(arg, lower.arg);
    }

    @Override
    public int hashCode() {
        return Objects.hash(arg);
    }

    @Override
    public String toString() {
        return "LOWER(" + arg + ")";
    }

    public @NotNull Expression<V> getArg() {
        return arg;
    }

    @Override
    public @NotNull ParametrizedSql toSql() {
        final ParametrizedSql sql = arg.toSql();
        return new ParametrizedSql("lower(" + sql.getSql92() + ")", sql.getSql92Parameters());
    }
}
