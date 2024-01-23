package com.gitlab.mvysny.jdbiorm.condition;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.BiPredicate;

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

    public enum Operator implements BiPredicate<Object, Object> {
        EQ("=") {
            @Override
            public boolean test(Object o, Object o2) {
                // in SQL, null is not equal to anything
                return o != null && o.equals(o2);
            }
        }, LT("<") {
            @Override
            public boolean test(Object o, Object o2) {
                return o != null && o2 != null && ((Comparable) o).compareTo((Comparable) o2) < 0;
            }
        }, LE("<=") {
            @Override
            public boolean test(Object o, Object o2) {
                return o != null && o2 != null && ((Comparable) o).compareTo((Comparable) o2) <= 0;
            }
        }, GT(">") {
            @Override
            public boolean test(Object o, Object o2) {
                return o != null && o2 != null && ((Comparable) o).compareTo((Comparable) o2) > 0;
            }
        }, GE(">=") {
            @Override
            public boolean test(Object o, Object o2) {
                return o != null && o2 != null && ((Comparable) o).compareTo((Comparable) o2) >= 0;
            }
        }, NE("<>") {
            @Override
            public boolean test(Object o, Object o2) {
                return o != null && o2 != null && !o.equals(o2);
            }
        };
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

    @Override
    public @NotNull ParametrizedSql toSql() {
        return ParametrizedSql.mergeWithOperator(operator.sql92Operator, arg1.toSql(), arg2.toSql());
    }

    @Override
    public boolean test(@NotNull Object row) {
        return operator.test(arg1.calculate(row), arg2.calculate(row));
    }
}
