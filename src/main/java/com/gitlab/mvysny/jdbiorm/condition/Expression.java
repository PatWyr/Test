package com.gitlab.mvysny.jdbiorm.condition;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * A SQL expression, e.g. a String constant or an operation such as "lower()". Immutable.
 * <h3>Implementation</h3>
 * {@link Object#equals(Object)}/{@link Object#hashCode()} must be implemented properly, so that the conditions can be
 * placed in a set. As a bare minimum, the filter type, the property name and the value which we compare against must be
 * taken into consideration.
 * <p></p>
 * {@link Object#toString()} must be implemented, to ease app debugging.
 * @param <V> the result type of the expression.
 */
public interface Expression<V> extends Serializable {

    @NotNull
    default Expression<V> lower() {
        return new Lower<>(this);
    }

    /**
     * A value constant expression.
     * @param <V> the value type
     */
    class Value<V> implements Expression<V> {
        @Nullable
        private final V value;

        public Value(@Nullable V value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return Objects.toString(value);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Value<?> value1 = (Value<?>) o;
            return Objects.equals(value, value1.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }
    }

    /**
     * The <code>EQ</code> operator.
     */
    @NotNull
    default Condition eq(@Nullable V value) {
        return eq(new Value<>(value));
    }

    /**
     * The <code>EQ</code> operator.
     */
    @NotNull
    default Condition eq(@NotNull Expression value) {
        return new Eq(this, value);
    }

    /**
     * The <code>LT</code> operator.
     */
    @NotNull
    default Condition lt(@Nullable V value) {
        return lt(new Value<>(value));
    }

    /**
     * The <code>LT</code> operator.
     */
    @NotNull
    default Condition lt(@NotNull Expression<? extends V> value) {
        return new Op(this, value, Op.Operator.LT);
    }

    /**
     * The <code>LE</code> operator.
     */
    @NotNull
    default Condition le(@Nullable V value) {
        return le(new Value<>(value));
    }

    /**
     * The <code>LE</code> operator.
     */
    @NotNull
    default Condition le(@NotNull Expression<? extends V> value) {
        return new Op(this, value, Op.Operator.LE);
    }

    /**
     * The <code>GT</code> operator.
     */
    @NotNull
    default Condition gt(@Nullable V value) {
        return gt(new Value<>(value));
    }

    /**
     * The <code>GT</code> operator.
     */
    @NotNull
    default Condition gt(@NotNull Expression<? extends V> value) {
        return new Op(this, value, Op.Operator.GT);
    }

    /**
     * The <code>GE</code> operator.
     */
    @NotNull
    default Condition ge(@Nullable V value) {
        return ge(new Value<>(value));
    }

    /**
     * The <code>GE</code> operator.
     */
    @NotNull
    default Condition ge(@NotNull Expression<? extends V> value) {
        return new Op(this, value, Op.Operator.GE);
    }

    /**
     * The <code>NE</code> operator.
     */
    @NotNull
    default Condition ne(@Nullable V value) {
        return ne(new Value<>(value));
    }

    /**
     * The <code>NE</code> operator.
     */
    @NotNull
    default Condition ne(@NotNull Expression<? extends V> value) {
        return new Op(this, value, Op.Operator.NE);
    }

    /**
     * Create a condition to check this field against several values.
     * <p>
     * SQL: <code>this in (values…)</code>
     * <p>
     * Note that generating dynamic SQL with arbitrary-length <code>IN</code>
     * predicates can cause cursor cache contention in some databases that use
     * unique SQL strings as a statement identifier (e.g. Oracle). In order to prevent such problems, you could
     * use TODO to produce less distinct SQL
     * strings (see also
     * <a href="https://github.com/jOOQ/jOOQ/issues/5600">[#5600]</a>), or you
     * could avoid <code>IN</code> lists, and replace them with:
     * <ul>
     * <li><code>IN</code> predicates on temporary tables</li>
     * <li><code>IN</code> predicates on unnested array bind variables</li>
     * </ul>
     */
    @NotNull
    default Condition in(@NotNull Collection<? extends V> values) {
        return new In(this, values.stream().map(Value::new).collect(Collectors.toSet()));
    }

    /**
     * Create a condition to check this field against several values.
     * <p>
     * SQL: <code>this in (values…)</code>
     * <p>
     * Note that generating dynamic SQL with arbitrary-length <code>IN</code>
     * predicates can cause cursor cache contention in some databases that use
     * unique SQL strings as a statement identifier (e.g. Oracle). In order to prevent such problems, you could
     * use TODO to produce less distinct SQL
     * strings (see also
     * <a href="https://github.com/jOOQ/jOOQ/issues/5600">[#5600]</a>), or you
     * could avoid <code>IN</code> lists, and replace them with:
     * <ul>
     * <li><code>IN</code> predicates on temporary tables</li>
     * <li><code>IN</code> predicates on unnested array bind variables</li>
     * </ul>
     */
    @NotNull
    default Condition in(@NotNull V... values) {
        return in(Arrays.asList(values));
    }

    /**
     * Create a condition to check this field against several values.
     * <p>
     * SQL: <code>this in (values…)</code>
     * <p>
     * Note that generating dynamic SQL with arbitrary-length <code>IN</code>
     * predicates can cause cursor cache contention in some databases that use
     * unique SQL strings as a statement identifier (e.g. Oracle). In order to prevent such problems, you could
     * use TODO to produce less distinct SQL
     * strings (see also
     * <a href="https://github.com/jOOQ/jOOQ/issues/5600">[#5600]</a>), or you
     * could avoid <code>IN</code> lists, and replace them with:
     * <ul>
     * <li><code>IN</code> predicates on temporary tables</li>
     * <li><code>IN</code> predicates on unnested array bind variables</li>
     * </ul>
     */
    @NotNull
    default Condition in(@NotNull Expression<? extends V>... values) {
        return new In(this, Arrays.asList(values));
    }
    /**
     * Create a condition to check this field against several values.
     * <p>
     * SQL: <code>this in (values…)</code>
     * <p>
     * Note that generating dynamic SQL with arbitrary-length <code>IN</code>
     * predicates can cause cursor cache contention in some databases that use
     * unique SQL strings as a statement identifier (e.g. Oracle). In order to prevent such problems, you could
     * use TODO to produce less distinct SQL
     * strings (see also
     * <a href="https://github.com/jOOQ/jOOQ/issues/5600">[#5600]</a>), or you
     * could avoid <code>IN</code> lists, and replace them with:
     * <ul>
     * <li><code>IN</code> predicates on temporary tables</li>
     * <li><code>IN</code> predicates on unnested array bind variables</li>
     * </ul>
     */
    @NotNull
    default Condition notIn(@NotNull Collection<? extends V> values) {
        return in(values).not();
    }

    /**
     * Create a condition to check this field against several values.
     * <p>
     * SQL: <code>this in (values…)</code>
     * <p>
     * Note that generating dynamic SQL with arbitrary-length <code>IN</code>
     * predicates can cause cursor cache contention in some databases that use
     * unique SQL strings as a statement identifier (e.g. Oracle). In order to prevent such problems, you could
     * use TODO to produce less distinct SQL
     * strings (see also
     * <a href="https://github.com/jOOQ/jOOQ/issues/5600">[#5600]</a>), or you
     * could avoid <code>IN</code> lists, and replace them with:
     * <ul>
     * <li><code>IN</code> predicates on temporary tables</li>
     * <li><code>IN</code> predicates on unnested array bind variables</li>
     * </ul>
     */
    @NotNull
    default Condition notIn(@NotNull V... values) {
        return in(values).not();
    }

    /**
     * Create a condition to check this field against several values.
     * <p>
     * SQL: <code>this in (values…)</code>
     * <p>
     * Note that generating dynamic SQL with arbitrary-length <code>IN</code>
     * predicates can cause cursor cache contention in some databases that use
     * unique SQL strings as a statement identifier (e.g. Oracle). In order to prevent such problems, you could
     * use TODO to produce less distinct SQL
     * strings (see also
     * <a href="https://github.com/jOOQ/jOOQ/issues/5600">[#5600]</a>), or you
     * could avoid <code>IN</code> lists, and replace them with:
     * <ul>
     * <li><code>IN</code> predicates on temporary tables</li>
     * <li><code>IN</code> predicates on unnested array bind variables</li>
     * </ul>
     */
    @NotNull
    default Condition notIn(@NotNull Expression<? extends V>... values) {
        return in(values).not();
    }

    @NotNull
    default Condition between(@Nullable V minValue, @Nullable V maxValue) {
        if (minValue == null) {
            return le(maxValue);
        } else if (maxValue == null) {
            return ge(minValue);
        } else {
            return ge(minValue).and(le(maxValue));
        }
    }

    @NotNull
    default Condition between(@Nullable Expression<? extends V> minValue, @Nullable Expression<? extends V> maxValue) {
        if (minValue == null) {
            return le(maxValue);
        } else if (maxValue == null) {
            return ge(minValue);
        } else {
            return ge(minValue).and(le(maxValue));
        }
    }

    @NotNull
    default Condition notBetween(@Nullable V minValue, @Nullable V maxValue) {
        return between(minValue, maxValue).not();
    }

    @NotNull
    default Condition notBetween(@Nullable Expression<? extends V> minValue, @Nullable Expression<? extends V> maxValue) {
        return between(minValue, maxValue).not();
    }

    /**
     * Create a condition to check this field against known string literals for
     * <code>true</code>.
     * <p>
     * SQL:
     * <code>lower(this) in ("1", "y", "yes", "true", "on", "enabled")</code>
     */
    @NotNull
    default Condition isTrue() {
        return new IsTrue(this);
    }

    /**
     * Create a condition to check this field against known string literals for
     * <code>false</code>.
     * <p>
     * SQL:
     * <code>lower(this) in ("0", "n", "no", "false", "off", "disabled")</code>
     */
    @NotNull
    default Condition isFalse() {
        return new IsFalse(this);
    }

    /**
     * <code>lower(this) = lower(value)</code>.
     */
    @NotNull
    default Condition equalIgnoreCase(@Nullable String value) {
        return equalIgnoreCase(new Value<>(value));
    }

    /**
     * <code>lower(this) = lower(value)</code>.
     */
    @NotNull
    default Condition equalIgnoreCase(@NotNull Expression<String> value) {
        return new Eq(lower(), value.lower());
    }

    /**
     * <code>lower(this) != lower(value)</code>.
     */
    @NotNull
    default Condition notEqualIgnoreCase(String value) {
        return notEqualIgnoreCase(new Value<>(value));
    }

    /**
     * <code>lower(this) != lower(value)</code>.
     */
    @NotNull
    default Condition notEqualIgnoreCase(@NotNull Expression<String> value) {
        return new Op(lower(), value.lower(), Op.Operator.NE);
    }
    /**
     * The <code>LIKE</code> operator.
     *
     * @param pattern e.g. "%foo%"
     */
    @NotNull
    default Condition like(@Nullable String pattern) {
        return like(new Value<>(pattern));
    }

    /**
     * The <code>LIKE</code> operator.
     */
    @NotNull
    default Condition like(@NotNull Expression<String> pattern) {
        return new Like(this, pattern);
    }

    /**
     * The <code>ILIKE</code> operator.
     *
     * @param pattern e.g. "%foo%"
     */
    @NotNull
    default Condition likeIgnoreCase(@Nullable String pattern) {
        return likeIgnoreCase(new Value<>(pattern));
    }

    /**
     * The <code>ILIKE</code> operator.
     */
    @NotNull
    default Condition likeIgnoreCase(@NotNull Expression<String> pattern) {
        return new LikeIgnoreCase(this, pattern);
    }

    /**
     * The <code>IS_NULL</code> operator.
     */
    @NotNull
    default Condition isNull() {
        return new IsNull(this);
    }

    /**
     * The <code>IS_NOT_NULL</code> operator.
     */
    @NotNull
    default Condition isNotNull() {
        return new IsNotNull(this);
    }
}
