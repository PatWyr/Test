package com.gitlab.mvysny.jdbiorm.condition;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;

/**
 * The CAST(arg1 as TYPE) expression.
 * @param <V> the new value type.
 */
public final class Cast<V> implements Expression<V> {
    /**
     * This expression is going to be cast.
     */
    @NotNull
    private final Expression<?> arg1;
    /**
     * The value is cast to this SQL type, for example <code>VARCHAR</code>.
     */
    @NotNull
    private final String sqlType;
    /**
     * The Java type corresponding to {@link #sqlType}.
     */
    @NotNull
    private final Class<V> valueClass;

    public Cast(@NotNull Expression<?> arg1, @NotNull String sqlType, @NotNull Class<V> valueClass) {
        this.arg1 = Objects.requireNonNull(arg1);
        this.sqlType = Objects.requireNonNull(sqlType);
        this.valueClass = Objects.requireNonNull(valueClass);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Cast)) return false;
        Cast<?> cast = (Cast<?>) o;
        return Objects.equals(arg1, cast.arg1) && Objects.equals(sqlType, cast.sqlType) && Objects.equals(valueClass, cast.valueClass);
    }

    @Override
    public int hashCode() {
        return Objects.hash(arg1, sqlType, valueClass);
    }

    @Override
    public String toString() {
        return "CAST(" + arg1 + " AS " + sqlType + ")";
    }

    /**
     * @return This expression is going to be cast.
     */
    @NotNull
    public Expression<?> getArg1() {
        return arg1;
    }

    /**
     * @return The value is cast to this SQL type, for example <code>VARCHAR</code>.
     */
    @NotNull
    public String getSqlType() {
        return sqlType;
    }

    /**
     * @return The Java type corresponding to {@link #sqlType}.
     */
    @NotNull
    public Class<V> getValueClass() {
        return valueClass;
    }

    @Override
    public @Nullable Object calculate(@NotNull Object row) {
        final Object value = arg1.calculate(row);
        if (value == null) {
            return null;
        }
        if (value.getClass() == valueClass) {
            return valueClass.cast(value);
        }
        if (valueClass == String.class) {
            return value.toString();
        }
        if (valueClass == Long.class || valueClass == long.class) {
            return Long.valueOf(value.toString());
        }
        if (valueClass == Integer.class || valueClass == int.class) {
            return Integer.valueOf(value.toString());
        }
        if (valueClass == Short.class || valueClass == short.class) {
            return Short.valueOf(value.toString());
        }
        if (valueClass == Byte.class || valueClass == byte.class) {
            return Byte.valueOf(value.toString());
        }
        if (valueClass == Character.class || valueClass == char.class) {
            final String str = value.toString();
            return str.isEmpty() ? '\0' : str.charAt(0);
        }
        if (valueClass == Float.class || valueClass == float.class) {
            return Float.valueOf(value.toString());
        }
        if (valueClass == Double.class || valueClass == double.class) {
            return Double.valueOf(value.toString());
        }
        if (valueClass == BigInteger.class) {
            return new BigInteger(value.toString());
        }
        if (valueClass == BigDecimal.class) {
            return new BigDecimal(value.toString());
        }
        return valueClass.cast(value);
    }

    @Override
    public @NotNull ParametrizedSql toSql() {
        final ParametrizedSql sql = arg1.toSql();
        return new ParametrizedSql("CAST((" + sql.getSql92() + ") AS " + sqlType + ")", sql.getSql92Parameters());
    }
}
