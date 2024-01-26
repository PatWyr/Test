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
 * <h3>Example of use</h3>
 * You start creating expressions usually by starting with a {@link com.gitlab.mvysny.jdbiorm.TableProperty TableProperty} (which itself
 * is an Expression); alternatively you can convert a constant literal into an Expression by calling
 * <code>new Expression.Value(5)</code>. Then you use some of the default Expression
 * functions to ultimately create a {@link Condition}, which you can then pass to DaoOfAny or Dao:
 * <code><pre>
 * Expression exp = Person.ID;
 * Condition condition = exp.eq(5);
 * List&lt;Person&gt; personnel = new Dao(Person.class).findAll(condition);
 * </pre></code>
 * Typically you call the above in shorthand form:
 * <code><pre>
 * List&lt;Person&gt; personnel = Person.dao.findAll(Person.ID.eq(5));
 * </pre></code>
 * <h3>Implementation</h3>
 * {@link Object#equals(Object)}/{@link Object#hashCode()} must be implemented properly, so that the conditions can be
 * placed in a set. As a bare minimum, the filter type, the property name and the value which we compare against must be
 * taken into consideration.
 * <p></p>
 * {@link Object#toString()} must be implemented, to ease app debugging.
 * @param <V> the result type of the expression.
 */
public interface Expression<V> extends Serializable {

    /**
     * The LOWER operator.
     * @return the expression which calculates LOWER of this.
     */
    @NotNull
    default Expression<V> lower() {
        return new Lower<>(this);
    }

    /**
     * A value constant expression.
     * @param <V> the value type
     */
    final class Value<V> implements Expression<V> {
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

        @Override
        public @NotNull ParametrizedSql toSql() {
            final String parameterName = ParametrizedSql.generateParameterName(this);
            return new ParametrizedSql(":" + parameterName, parameterName, value);
        }

        @Nullable
        public V getValue() {
            return value;
        }

        @Override
        public @Nullable Object calculate(@NotNull Object row) {
            return value;
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
    default Condition eq(@NotNull Expression<?> value) {
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
        return op(Op.Operator.LT, value);
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
     * @return the condition, not null.
     */
    @NotNull
    default Condition le(@NotNull Expression<? extends V> value) {
        return op(Op.Operator.LE, value);
    }

    /**
     * The <code>GT</code> operator.
     * @return the condition, not null.
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
        return op(Op.Operator.GT, value);
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
     * @return the condition, not null.
     */
    @NotNull
    default Condition ge(@NotNull Expression<? extends V> value) {
        return op(Op.Operator.GE, value);
    }

    /**
     * The <code>NE</code> operator.
     * @return the condition, not null.
     */
    @NotNull
    default Condition ne(@Nullable V value) {
        return ne(new Value<>(value));
    }

    /**
     * The <code>NE</code> operator.
     * @return the condition, not null.
     */
    @NotNull
    default Condition ne(@NotNull Expression<? extends V> value) {
        return op(Op.Operator.NE, value);
    }

    @NotNull
    default Condition op(@NotNull Op.Operator operator, @NotNull Expression<? extends V> value) {
        return new Op(this, value, operator);
    }

    @NotNull
    default Condition op(@NotNull Op.Operator operator, @Nullable V value) {
        return op(operator, new Value<>(value));
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
     * @return the condition, not null.
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
     * @return the condition, not null.
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
     * @return the condition, not null.
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
     * @return the condition, not null.
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
     * @return the condition, not null.
     */
    @NotNull
    default Condition notIn(@NotNull Expression<? extends V>... values) {
        return in(values).not();
    }

    /**
     * Create a condition which matches a value against min or max value, whichever is not null.
     * If both parameters are null, {@link Condition#NO_CONDITION} is returned.
     * @param minValue the min value; matched value must be equal to or greater than this value. If null, no lower bound is set.
     * @param maxValue the max value; matched value must be equal to or lower than this value. If null, no upper bound is set.
     * @return the condition, not null.
     */
    @NotNull
    default Condition between(@Nullable V minValue, @Nullable V maxValue) {
        return between(minValue == null ? null : new Value<>(minValue), maxValue == null ? null : new Value<>(maxValue));
    }

    /**
     * Create a condition which matches a value against min or max value, whichever is not null.
     * If both parameters are null, {@link Condition#NO_CONDITION} is returned.
     * @param minValue the min value; matched value must be equal to or greater than this value. If null, no lower bound is set.
     * @param maxValue the max value; matched value must be equal to or lower than this value. If null, no upper bound is set.
     * @return the condition, not null.
     */
    @NotNull
    default Condition between(@Nullable Expression<? extends V> minValue, @Nullable Expression<? extends V> maxValue) {
        if (minValue == null) {
            if (maxValue == null) {
                return Condition.NO_CONDITION;
            }
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
     * @return the condition, not null.
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
     * @return the condition, not null.
     */
    @NotNull
    default Condition isFalse() {
        return new IsFalse(this);
    }

    /**
     * Calls {@link #isTrue()} or {@link #isFalse()}, depending on the input value.
     * @param value the input value
     * @return the condition, not null.
     */
    @NotNull
    default Condition is(boolean value) {
        return value ? isTrue() : isFalse();
    }

    /**
     * <code>lower(this) = lower(value)</code>.
     * @return the condition, not null.
     */
    @NotNull
    default Condition equalIgnoreCase(@Nullable String value) {
        return equalIgnoreCase(new Value<>(value));
    }

    /**
     * <code>lower(this) = lower(value)</code>.
     * @return the condition, not null.
     */
    @NotNull
    default Condition equalIgnoreCase(@NotNull Expression<String> value) {
        return new Eq(lower(), value.lower());
    }

    /**
     * <code>lower(this) != lower(value)</code>.
     * @return the condition, not null.
     */
    @NotNull
    default Condition notEqualIgnoreCase(String value) {
        return notEqualIgnoreCase(new Value<>(value));
    }

    /**
     * <code>lower(this) != lower(value)</code>.
     * @return the condition, not null.
     */
    @NotNull
    default Condition notEqualIgnoreCase(@NotNull Expression<String> value) {
        return new Op(lower(), value.lower(), Op.Operator.NE);
    }
    /**
     * The <code>LIKE</code> operator.
     *
     * @param pattern e.g. "%foo%"
     * @return the condition, not null.
     */
    @NotNull
    default Condition like(@Nullable String pattern) {
        return like(new Value<>(pattern));
    }

    /**
     * The <code>LIKE</code> operator.
     * @param pattern e.g. "%foo%"
     * @return the condition, not null.
     */
    @NotNull
    default Condition like(@NotNull Expression<String> pattern) {
        return new Like(this, pattern);
    }

    /**
     * The <code>ILIKE</code> operator.
     *
     * @param pattern e.g. "%foo%"
     * @return the condition, not null.
     */
    @NotNull
    default Condition likeIgnoreCase(@Nullable String pattern) {
        return likeIgnoreCase(new Value<>(pattern));
    }

    /**
     * The <code>ILIKE</code> operator.
     * @return the condition, not null.
     */
    @NotNull
    default Condition likeIgnoreCase(@NotNull Expression<String> pattern) {
        return new LikeIgnoreCase(this, pattern);
    }

    /**
     * The <code>IS_NULL</code> operator.
     * @return the condition, not null.
     */
    @NotNull
    default Condition isNull() {
        return new IsNull(this);
    }

    /**
     * The <code>IS_NOT_NULL</code> operator.
     * @return the condition, not null.
     */
    @NotNull
    default Condition isNotNull() {
        return new IsNotNull(this);
    }

    /**
     * Converts this condition to a {@link ParametrizedSql} which can then be
     * passed to a SELECT. Used by DAOs to create proper SQL statements.
     * @return the WHERE SQL part, not null.
     */
    @NotNull
    ParametrizedSql toSql();

    /**
     * A FullText filter which performs the case-insensitive full-text search.
     * Any probe text must either contain all words in this query,
     * or the query words must match beginnings of all of the words contained in the probe string.
     * <p></p>
     * See the jdbi-orm README.md on how to configure a full-text search in a SQL database/RDBMS system.
     * @param query the user input, no need to normalize or remove special characters since
     *              this is performed automatically by {@link FullTextCondition}.
     *              If the query is blank or contains no searchable words, {@link Condition#NO_CONDITION}
     *              is returned.
     * @return the condition, not null.
     */
    @NotNull
    default Condition fullTextMatches(@NotNull String query) {
        return FullTextCondition.of(this, query);
    }

    /**
     * Calculates the value of this expression via a Java code (if possible).
     * @param row the row bean on which the expression is calculated, not null. Might be ignored by
     *            the implementation of this function (e.g. {@link Value} ignores it).
     * @return the calculated value
     * @throws IllegalStateException if the value can not be calculated via Java code.
     */
    @Nullable
    Object calculate(@NotNull Object row);

    /**
     * The <code>COALESCE</code> operator.
     * @param other other value, returned when this expression evaluates to null.
     * @return the COALESCE expression.
     */
    @NotNull
    default Expression<V> coalesce(@NotNull Expression<? extends V> other) {
        return new Coalesce<>(this, other);
    }

    /**
     * The <code>COALESCE</code> operator.
     * @param other other value, returned when this expression evaluates to null.
     * @return the COALESCE expression.
     */
    @NotNull
    default Expression<V> coalesce(@Nullable V other) {
        return coalesce(new Expression.Value<>(other));
    }

    /**
     * The <code>NULLIF</code> function.
     * @param other other value.
     * @return the NULLIF expression.
     */
    @NotNull
    default Expression<V> nullIf(@NotNull Expression<? extends V> other) {
        return new NullIf<>(this, other);
    }

    /**
     * The <code>NULLIF</code> function.
     * @param other other value.
     * @return the NULLIF expression.
     */
    @NotNull
    default Expression<V> nullIf(@Nullable V other) {
        return nullIf(new Expression.Value<>(other));
    }
}
