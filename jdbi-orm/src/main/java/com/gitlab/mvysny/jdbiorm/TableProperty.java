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
    /**
     * The class of the entity which owns this property.
     */
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

    /**
     * The class of the entity which owns this property.
     */
    @NotNull
    public Class<E> getEntityClass() {
        return entityClass;
    }

    @NotNull
    private PropertyMeta getMeta() {
        if (meta == null) {
            meta = EntityMeta.of(entityClass).getProperty(propertyName);
        }
        return meta;
    }

    @Override
    public @NotNull Class<?> getValueType() {
        return getMeta().getValueType();
    }

    @NotNull
    public static <E, V> TableProperty<E, V> of(@NotNull Class<E> entity, @NotNull Property.Name propertyName) {
        return new TableProperty<>(entity, propertyName);
    }

    /**
     * Creates a table property for given entity and property name.
     * @param entity the entity class.
     * @param propertyName the property name, see {@link Property.Name} for details on the naming scheme.
     * @return the table property.
     * @param <E> the entity type
     * @param <V> the property value type, e.g. Long or String.
     */
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

    @NotNull
    public String toExternalString() {
        return "TableProperty:" + entityClass.getName() + EXTERNAL_STRING_SEPARATOR + propertyName.getName();
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

    @Override
    public @Nullable Object calculate(@NotNull Object row) {
        Objects.requireNonNull(row);
        final E bean = entityClass.cast(row);
        return getMeta().get(bean);
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

        @NotNull
        public String toExternalString() {
            return "TablePropertyAlias:" + tableNameAlias + EXTERNAL_STRING_SEPARATOR + tableProperty.toExternalString();
        }

        @Override
        public @NotNull Class<?> getValueType() {
            return tableProperty.getValueType();
        }

        @Override
        public @NotNull ParametrizedSql toSql() {
            return new ParametrizedSql(getDbName().getQualifiedName());
        }

        @Override
        public @Nullable Object calculate(@NotNull Object row) {
            return tableProperty.calculate(row);
        }
    }
}
