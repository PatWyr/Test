package com.gitlab.mvysny.jdbiorm.condition;

import com.gitlab.mvysny.jdbiorm.JdbiOrm;
import com.gitlab.mvysny.jdbiorm.quirks.DatabaseVariant;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Set;

/**
 * Create a condition to check this field against known string literals for
 * <code>false</code>.
 * <p>
 * SQL:
 * <code>lower(this) in ("0", "n", "no", "false", "off", "disabled")</code>
 */
public final class IsFalse implements Condition {
    @NotNull
    private final Expression<?> arg;

    public IsFalse(@NotNull Expression<?> arg) {
        this.arg = Objects.requireNonNull(arg);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IsFalse isTrue = (IsFalse) o;
        return Objects.equals(arg, isTrue.arg);
    }

    @Override
    public int hashCode() {
        return Objects.hash(arg);
    }

    @Override
    public String toString() {
        return arg + " IS FALSE";
    }

    public @NotNull Expression<?> getArg() {
        return arg;
    }

    @Override
    public @NotNull ParametrizedSql toSql() {
        if (JdbiOrm.databaseVariant == DatabaseVariant.PostgreSQL) {
            return new Eq(arg, new Expression.Value<>(false)).toSql();
        }
        final ParametrizedSql sql = arg.toSql();
        return new ParametrizedSql("lower(" + sql.getSql92() + ") in ('0', 'n', 'no', 'false', 'off', 'disabled')", sql.getSql92Parameters());
    }

    @NotNull
    private static final Set<String> falseValues = Set.of("0", "n", "no", "false", "off", "disabled");

    @Override
    public boolean test() {
        return test(arg);
    }

    /**
     * Tests whether given expression calculates to false.
     * @param expression the expression to test, not null.
     * @return true if the expression calculated to false; false if the expression calculated to null or non-false value (e.g. 25, "true" etc).
     */
    public static boolean test(@NotNull Expression<?> expression) {
        final Object value = expression.calculate();
        return value != null && falseValues.contains(value.toString().toLowerCase(JdbiOrm.getLocale()));
    }
}
