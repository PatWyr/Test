package com.gitlab.mvysny.jdbiorm.condition;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class Op implements Condition {
    @NotNull
    private final Expression<?> arg1;
    @NotNull
    private final Expression<?> arg2;
    @NotNull
    private final Operator operator;

    public Op(@NotNull Expression<?> arg1, @NotNull Expression<?> arg2, @NotNull Operator operator) {
        this.arg1 = Objects.requireNonNull(arg1);
        this.arg2 = Objects.requireNonNull(arg2);
        this.operator = Objects.requireNonNull(operator);
    }

    public enum Operator {
        EQ("="), LT("<"), LE("<="), GT(">"), GE(">="), NE("<>");
        @NotNull
        public final String sql92Operator;

        Operator(@NotNull String sql92Operator) {
            this.sql92Operator = sql92Operator;
        }
    }

    @Override
    public String toString() {
        return arg1 + " " + operator.sql92Operator + " " + arg2;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Op op = (Op) o;
        return Objects.equals(arg1, op.arg1) && Objects.equals(arg2, op.arg2) && operator == op.operator;
    }

    @Override
    public int hashCode() {
        return Objects.hash(arg1, arg2, operator);
    }

    @NotNull
    public Expression<?> getArg1() {
        return arg1;
    }

    @NotNull
    public Expression<?> getArg2() {
        return arg2;
    }

    @NotNull
    public Operator getOperator() {
        return operator;
    }
}
