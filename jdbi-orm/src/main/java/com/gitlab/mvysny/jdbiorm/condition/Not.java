package com.gitlab.mvysny.jdbiorm.condition;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * The <code>NOT</code> statement.
 */
final class Not implements Condition {
    @NotNull
    private final Condition condition;

    public Not(@NotNull Condition condition) {
        this.condition = Objects.requireNonNull(condition);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Not not = (Not) o;
        return Objects.equals(condition, not.condition);
    }

    @Override
    public int hashCode() {
        return Objects.hash(condition);
    }

    @Override
    public String toString() {
        return "NOT(" + condition + ')';
    }

    @NotNull
    public Condition getCondition() {
        return condition;
    }

    @Override
    public @NotNull ParametrizedSql toSql() {
        final ParametrizedSql sql = condition.toSql();
        return new ParametrizedSql("NOT (" + sql.getSql92() + ")", sql.getSql92Parameters());
    }

    @Override
    public boolean test(@NotNull Object row) {
        return !condition.test(row);
    }
}
