package com.gitlab.mvysny.jdbiorm;

import com.gitlab.mvysny.jdbiorm.condition.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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
     * <p></p>
     * Immutable, thread-safe.
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
     * A fully qualified database name of a column.
     * The name is any of these:
     * <ul>
     * <li>The formal name of the field, if it is a <i>physical table/view
     * field</i>, including the table name, e.g. <code>PERSON.id</code></li>
     * <li>The alias of an <i>aliased field</i></li>
     * </ul>
     * Immutable, thread-safe.
     */
    final class DbName implements Serializable {
        /**
         * The name of the database table backed by this entity. Defaults to {@link Class#getSimpleName()}
         * (no conversion from `camelCase` to `hyphen_separated`)
         * but you can annotate your class with {@link Table} to override
         * that.
         * <p></p>
         * This may also be an alias, e.g. in <code>SELECT p.* from Person p</code>
         * this would be <code>p</code>.
         */
        @NotNull
        private final String tableName;

        /**
         * Represents the name of a column in a database table.
         */
        @NotNull
        private final String columnName;

        public DbName(@NotNull String tableName, @NotNull String columnName) {
            this.tableName = Objects.requireNonNull(tableName);
            this.columnName = Objects.requireNonNull(columnName);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DbName dbName = (DbName) o;
            return Objects.equals(tableName, dbName.tableName) && Objects.equals(columnName, dbName.columnName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(tableName, columnName);
        }

        /**
         * Returns the qualified name of the database column.
         * The qualified name is a combination of the table name and the column name,
         * separated by a dot (.) character.
         * <p></p>
         * Example: PERSON.id
         * @return the qualified name of the column
         */
        @NotNull
        public String getQualifiedName() {
            return tableName + "." + columnName;
        }

        /**
         * This is just the name of the column, exactly as present in the database.
         * @return the name of the column, without the name of the table.
         */
        @NotNull
        public String getUnqualifiedName() {
            return columnName;
        }

        @Override
        public String toString() {
            return getQualifiedName();
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
    DbName getDbName();

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
    default Condition eq(@NotNull Property<? extends V> value) {
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
    default Condition lt(@NotNull Property<? extends V> value) {
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
    default Condition le(@NotNull Property<? extends V> value) {
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
    default Condition gt(@NotNull Property<? extends V> value) {
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
    default Condition ge(@NotNull Property<? extends V> value) {
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
    default Condition ne(@NotNull Property<? extends V> value) {
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
    default Condition in(@NotNull Property<? extends V>... values) {
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
    default Condition notIn(@NotNull Property<? extends V>... values) {
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
    default Condition between(@Nullable Property<? extends V> minValue, @Nullable Property<? extends V> maxValue) {
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
    default Condition notBetween(@Nullable Property<? extends V> minValue, @Nullable Property<? extends V> maxValue) {
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

    @NotNull
    default Property<V> lower() {
        return new Lower(this);
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
    default Condition equalIgnoreCase(@NotNull Property<String> value) {
        return new Eq(lower(), value.lower());
    }

    /**
     * <code>lower(this) != lower(value)</code>.
     */
    @NotNull
    default Condition notEqualIgnoreCase(String value) {
        return equalIgnoreCase(value).not();
    }

    /**
     * <code>lower(this) != lower(value)</code>.
     */
    @NotNull
    default Condition notEqualIgnoreCase(@NotNull Property<String> value) {
        return equalIgnoreCase(value).not();
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
    default Condition like(@NotNull Property<String> pattern) {
        return new Like(this, pattern);
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
        public @NotNull DbName getDbName() {
            throw new UnsupportedOperationException("Value has no name");
        }
    }
}
