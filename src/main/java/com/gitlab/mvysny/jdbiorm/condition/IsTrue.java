package com.gitlab.mvysny.jdbiorm.condition;

import com.gitlab.mvysny.jdbiorm.JdbiOrm;
import com.gitlab.mvysny.jdbiorm.quirks.DatabaseVariant;
import org.jdbi.v3.core.Jdbi;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Create a condition to check this field against known string literals for
 * <code>true</code>.
 * <p>
 * SQL:
 * <code>lower(this) in ("1", "y", "yes", "true", "on", "enabled")</code>
 */
public final class IsTrue implements Condition {
    @NotNull
    private final Expression<?> arg;

    public IsTrue(@NotNull Expression<?> arg) {
        this.arg = Objects.requireNonNull(arg);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IsTrue isTrue = (IsTrue) o;
        return Objects.equals(arg, isTrue.arg);
    }

    @Override
    public int hashCode() {
        return Objects.hash(arg);
    }

    @Override
    public String toString() {
        return arg + " IS TRUE";
    }

    public @NotNull Expression<?> getArg() {
        return arg;
    }

    @Override
    public @NotNull ParametrizedSql toSql() {
        if (JdbiOrm.databaseVariant == DatabaseVariant.PostgreSQL) {
            return new Eq(arg, new Expression.Value<>(true)).toSql();
        }
        final ParametrizedSql sql = arg.toSql();
        return new ParametrizedSql("lower(" + sql.getSql92() + ") in ('1', 'y', 'yes', 'true', 'on', 'enabled')", sql.getSql92Parameters());
    }
}
