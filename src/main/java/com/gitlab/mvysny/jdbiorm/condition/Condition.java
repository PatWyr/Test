package com.gitlab.mvysny.jdbiorm.condition;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;

/**
 * A SQL condition. Immutable.
 * <h3>Implementation</h3>
 * {@link Object#equals(Object)}/{@link Object#hashCode()} must be implemented properly, so that the conditions can be
 * placed in a set. As a bare minimum, the filter type, the property name and the value which we compare against must be
 * taken into consideration.
 * <p></p>
 * {@link Object#toString()} must be implemented, to ease app debugging. This is not necessarily a valid SQL92 WHERE
 * clause.
 **/
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
        return other == null ? this : new Or(this, other);
    }

    @NotNull
    default Condition and(@Nullable Condition other) {
        return other == null ? this : new And(this, other);
    }

    @NotNull
    default Condition xor(@Nullable Condition other) {
        return other == null ? this : new Xor(this, other);
    }
}
