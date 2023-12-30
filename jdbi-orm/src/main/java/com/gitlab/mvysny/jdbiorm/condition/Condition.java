package com.gitlab.mvysny.jdbiorm.condition;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;

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
 */
public interface Condition extends Serializable {
    /**
     * The <code>NOT</code> operator.
     */
    @NotNull
    default Condition not() {
        return new Not(this);
    }

    @NotNull
    default Condition or(@Nullable Condition other) {
        return other == null || other == NO_CONDITION ? this : new Or(this, other);
    }

    @NotNull
    default Condition and(@Nullable Condition other) {
        return other == null || other == NO_CONDITION ? this : new And(this, other);
    }

    /**
     * Produces a SQL statement from this condition.
     * @return a SQL statement, never null. Some conditions (notably {@link #NO_CONDITION})
     * can not be turned into a SQL statement.
     */
    @NotNull
    ParametrizedSql toSql();

    /**
     * {@link NoCondition}.
     */
    @NotNull
    Condition NO_CONDITION = NoCondition.INSTANCE;
}
