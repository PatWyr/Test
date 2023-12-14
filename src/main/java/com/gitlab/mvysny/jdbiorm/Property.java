package com.gitlab.mvysny.jdbiorm;

import com.gitlab.mvysny.jdbiorm.condition.Condition;
import com.gitlab.mvysny.jdbiorm.condition.Eq;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
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
     * The full name of a property. Most often the {@link Field#getName() Java field name} from an entity bean.
     * However, in case of composite IDs this is a comma-separated {@link #getNamePath()} e.g. "id.component1".
     */
    final class Name implements Serializable {
        @NotNull
        private final String name;

        public Name(@NotNull String name) {
            if (name.isBlank()) {
                throw new IllegalArgumentException("Parameter name: can not be blank");
            }
            // verify that name doesn't contain funny characters
            if (name.indexOf(' ') >= 0 || name.indexOf('\'') >= 0 || name.indexOf('"') >= 0) {
                throw new IllegalArgumentException("Parameter name: invalid value " + name + ": must not contain weird characters");
            }
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Name name1 = (Name) o;
            return Objects.equals(name, name1.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }

        @Override
        public String toString() {
            return name;
        }

        /**
         * @return The full name of a property. Most often the {@link Field#getName() Java field name} from an entity bean.
         * However, in case of composite IDs this is a comma-separated {@link #getNamePath()} e.g. "id.component1".
         */
        @NotNull
        public String getName() {
            return name;
        }

        @NotNull
        public List<String> getNamePath() {
            return Arrays.asList(name.split("\\."));
        }
    }

    /**
     * The full name of a property. Most often the {@link Field#getName() Java field name} from an entity bean.
     * However, in case of composite IDs this is a comma-separated value such as <code>id.component1</code>.
     * @return the name
     */
    @NotNull
    Name getName();

    /**
     * The database name of this field. This is the column name which must be used in the WHERE clauses.
     * <p></p>
     * The name is any of these:
     * <ul>
     * <li>The formal name of the field, if it is a <i>physical table/view
     * field</i>, including the table name, e.g. PERSON.id</li>
     * <li>The alias of an <i>aliased field</i></li>
     * </ul>
     * @return the database column name, not null.
     */
    @NotNull
    String getQualifiedName();

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

        @Override
        public @NotNull Name getName() {
            throw new UnsupportedOperationException("Value has no name");
        }

        @Override
        public @NotNull String getQualifiedName() {
            throw new UnsupportedOperationException("Value has no name");
        }
    }
}
