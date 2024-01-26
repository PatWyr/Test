package com.gitlab.mvysny.jdbiorm.condition;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.function.Predicate;

/**
 * A SQL condition. Immutable. To use in your project, add table properties to your entities; see {@link com.gitlab.mvysny.jdbiorm.TableProperty} for more information.
 * Then, you can write <code>Person.NAME.eq("John")</code> to create a condition which you can
 * pass to {@link com.gitlab.mvysny.jdbiorm.DaoOfAny#findAllBy(Condition)}.
 * <h3>Implementation</h3>
 * {@link Object#equals(Object)}/{@link Object#hashCode()} must be implemented properly, so that the conditions can be
 * placed in a set. As a bare minimum, the filter type, the property name and the value which we compare against must be
 * taken into consideration.
 * <p></p>
 * {@link Object#toString()} must be implemented, to ease app debugging. This is not necessarily a valid SQL92 WHERE
 * clause.
 * <p></p>
 * Use {@link com.gitlab.mvysny.jdbiorm.JdbiOrm#databaseVariant} if you need to emit different SQL
 * for particular database.
 */
public interface Condition extends Serializable, Predicate<Object> {
    /**
     * The <code>NOT</code> operator.
     * @return this condition, negated.
     */
    @NotNull
    default Condition not() {
        return new Not(this);
    }

    /**
     * The <code>OR</code> operator. x OR {@link Condition#NO_CONDITION} returns x.
     * @return this condition OR given condition.
     */
    @NotNull
    default Condition or(@Nullable Condition other) {
        return other == null || other == NO_CONDITION || this.equals(other) ? this : new Or(this, other);
    }

    /**
     * The <code>AND</code> operator. x AND {@link Condition#NO_CONDITION} returns x.
     * @return this condition AND given condition.
     */
    @NotNull
    default Condition and(@Nullable Condition other) {
        return other == null || other == NO_CONDITION || this.equals(other) ? this : new And(this, other);
    }

    /**
     * Produces a SQL statement from this condition.
     * @return a SQL statement, never null. Some conditions (notably {@link #NO_CONDITION})
     * can not be turned into a SQL statement.
     */
    @NotNull
    ParametrizedSql toSql();

    /**
     * Calculates the value of this expression via a Java code (if possible).
     * @param row the row bean on which the expression is calculated, not null. Might be ignored by
     *            the implementation of this function (e.g. {@link Expression.Value} ignores it).
     * @return the calculated value
     * @throws IllegalStateException if the value can not be calculated via Java code.
     */
    boolean test(@NotNull Object row);

    /**
     * {@link NoCondition}.
     */
    @NotNull
    Condition NO_CONDITION = NoCondition.INSTANCE;
}
