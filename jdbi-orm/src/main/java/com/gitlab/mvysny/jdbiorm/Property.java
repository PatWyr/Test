package com.gitlab.mvysny.jdbiorm;

import com.gitlab.mvysny.jdbiorm.condition.*;
import org.jetbrains.annotations.NotNull;

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
 *
 * @param <V> the value of this property
 */
public interface Property<V> extends Expression<V> {

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
     * A fully-qualified database name of a column.
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
         *
         * @return the qualified name of the column
         */
        @NotNull
        public String getQualifiedName() {
            return tableName + "." + columnName;
        }

        /**
         * This is just the name of the column, exactly as present in the database.
         *
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
     *
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
     *
     * @return the database column name, not null.
     */
    @NotNull
    DbName getDbName();

    /**
     * Returns the sorting clause which uses ascending order over this property.
     * @return the sorting clause.
     */
    @NotNull
    default OrderBy asc() {
        return orderBy(OrderBy.ASC);
    }

    /**
     * Returns the sorting clause which uses descending order over this property.
     * @return the sorting clause.
     */
    @NotNull
    default OrderBy desc() {
        return orderBy(OrderBy.DESC);
    }

    /**
     * Returns the sorting clause which uses given order over this property.
     * @param order the sorting order to use.
     * @return the sorting clause.
     */
    @NotNull
    default OrderBy orderBy(@NotNull OrderBy.Order order) {
        return new OrderBy(this, order);
    }

    /**
     * Produces an externalizable String in the form of <code>TableProperty:entityFullClassName propertyName [tableAlias]</code>
     *
     * @return externalizable String which can be parsed back via {@link #fromExternalString(String)}.
     */
    @NotNull
    String toExternalString();

    @NotNull
    static Property<?> fromExternalString(@NotNull String externalForm) {
        if (externalForm.startsWith("TableProperty:")) {
            final String[] parts = externalForm.substring(14).split("\\s+");
            try {
                return TableProperty.of(Class.forName(parts[0]), parts[1]);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        } else if (externalForm.startsWith("TablePropertyAlias:")) {
            final String[] parts = externalForm.substring(19).split("\\s+", 2);
            final String alias = parts[0];
            final TableProperty<?, ?> p = (TableProperty<?, ?>) fromExternalString(parts[1]);
            return p.tableAlias(alias);
        } else {
            throw new IllegalArgumentException("Parameter externalForm: invalid value " + externalForm + ": unsupported form");
        }
    }

    /**
     * Returns the type of the value of this property. Beware: this could also be a primitive type
     * (e.g. <code>int.class</code>/{@link Integer#TYPE}).
     * @return the type of the value, not null.
     */
    @NotNull
    Class<?> getValueType();
}
