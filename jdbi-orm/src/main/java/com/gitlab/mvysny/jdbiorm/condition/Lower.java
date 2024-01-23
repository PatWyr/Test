package com.gitlab.mvysny.jdbiorm.condition;

import com.gitlab.mvysny.jdbiorm.JdbiOrm;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * The LOWER operator.
 * @param <V> the type of the accepted value, usually String.
 */
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
        return new ParametrizedSql("LOWER(" + sql.getSql92() + ")", sql.getSql92Parameters());
    }

    @Override
    public @Nullable Object calculate(@NotNull Object row) {
        final Object value = arg.calculate(row);
        if (value == null) {
            return null;
        }
        if (!(value instanceof String)) {
            throw new IllegalStateException("Expression " + arg + " doesn't produce String, can't calculate lower(): " + value);
        }
        return ((String) value).toLowerCase(JdbiOrm.getLocale());
    }
}
