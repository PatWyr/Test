package com.gitlab.mvysny.jdbiorm;

import com.gitlab.mvysny.jdbiorm.condition.ParametrizedSql;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * A field contained in a table. To define table properties for your entity,
 * add a static Java field for every property. For example, if the Person entity
 * has a String name, then the static Java field would look like this:
 * <pre>
 * {@literal @JdbiProperty(map = false)}
 * public static final TableProperty<Person, String> NAME = TableProperty.of(Person.class, "name");
 * </pre>
 * Now you can create SQL queries programmatically, e.g.
 * <pre>
 * Person.dao.findAllBy(Person.NAME.eq("John"));
 * </pre>
 * @param <E> the entity type.
 * @param <V> the value type of this field.
 */
public final class TableProperty<E, V> implements Property<V> {
    @NotNull
    private final Class<E> entityClass;
    @NotNull
    private final Property.Name propertyName;

    private TableProperty(@NotNull Class<E> entity, @NotNull Property.Name propertyName) {
        this.entityClass = Objects.requireNonNull(entity);
        this.propertyName = Objects.requireNonNull(propertyName);
        getMeta(); // check that the property exists
    }

    @Nullable
    private transient volatile PropertyMeta meta;

    @NotNull
    private PropertyMeta getMeta() {
        if (meta == null) {
            meta = EntityMeta.of(entityClass).getProperty(propertyName);
        }
        return meta;
    }

    @NotNull
    public static <E, V> TableProperty<E, V> of(@NotNull Class<E> entity, @NotNull Property.Name propertyName) {
        return new TableProperty<>(entity, propertyName);
    }

    @NotNull
    public static <E, V> TableProperty<E, V> of(@NotNull Class<E> entity, @NotNull String propertyName) {
        return new TableProperty<>(entity, new Property.Name(propertyName));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TableProperty<?, ?> that = (TableProperty<?, ?>) o;
        return Objects.equals(entityClass, that.entityClass) && Objects.equals(propertyName, that.propertyName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entityClass, propertyName);
    }

    @Override
    public String toString() {
        return entityClass.getSimpleName() + "." + propertyName;
    }

    @Override
    public @NotNull Name getName() {
        return propertyName;
    }

    @Override
    public @NotNull DbName getDbName() {
        return getMeta().getDbName();
    }

    @NotNull
    public Property<V> tableAlias(@NotNull String tableNameAlias) {
        return new AliasedTableProperty<>(this, tableNameAlias);
    }

    @Override
    public @NotNull ParametrizedSql toSql() {
        return new ParametrizedSql(getDbName().getQualifiedName());
    }

    private static final class AliasedTableProperty<E, V> implements Property<V> {
        @NotNull
        private final TableProperty<E, V> tableProperty;
        @NotNull
        private final String tableNameAlias;

        private AliasedTableProperty(@NotNull TableProperty<E, V> tableProperty, @NotNull String tableNameAlias) {
            this.tableProperty = Objects.requireNonNull(tableProperty);
            this.tableNameAlias = Objects.requireNonNull(tableNameAlias);
        }

        @Override
        public @NotNull Name getName() {
            return tableProperty.getName();
        }

        @Override
        public @NotNull DbName getDbName() {
            return new DbName(tableNameAlias, tableProperty.getDbName().getUnqualifiedName());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            AliasedTableProperty<?, ?> that = (AliasedTableProperty<?, ?>) o;
            return Objects.equals(tableProperty, that.tableProperty) && Objects.equals(tableNameAlias, that.tableNameAlias);
        }

        @Override
        public int hashCode() {
            return Objects.hash(tableProperty, tableNameAlias);
        }

        @Override
        public String toString() {
            return tableProperty.entityClass.getSimpleName() + " " + tableNameAlias + "." + tableProperty.getName();
        }

        @Override
        public @NotNull ParametrizedSql toSql() {
            return new ParametrizedSql(getDbName().getQualifiedName());
        }
    }
}
