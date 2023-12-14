package com.gitlab.mvysny.jdbiorm;

import com.gitlab.mvysny.jdbiorm.condition.Condition;
import com.gitlab.mvysny.jdbiorm.condition.Eq;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.Objects;

/**
 * An entity property. Akin to JOOQ Field.
 * <h3>Implementation</h3>
 * {@link Object#equals(Object)}/{@link Object#hashCode()} must be implemented properly, so that the conditions can be
 * placed in a set. As a bare minimum, the filter type, the property name and the value which we compare against must be
 * taken into consideration.
 * <p></p>
 * {@link Object#toString()} must be implemented, to ease app debugging. This is not necessarily a valid SQL92 WHERE
 * clause.
 * @param <V> the value of this property
 */
public interface Property<V> extends Serializable {

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
    default Condition eq(@NotNull Property<V> value) {
        return new Eq(this, value);
    }

    /**
     * A special implementation of property that is not a property of any particular entity,
     * but rather a value constant.
     * @param <V> the value type
     */
    class Value<V> implements Property<V> {
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
}
