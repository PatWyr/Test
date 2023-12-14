package com.gitlab.mvysny.jdbiorm;

import org.jdbi.v3.core.mapper.reflect.ColumnName;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * A field contained in a table.
 * @param <E> the entity type.
 * @param <V> the value type of this field.
 */
public final class TableProperty<E, V> implements Property<V> {
    @NotNull
    private final Class<E> entityClass;
    @NotNull
    private final String propertyName;

    private TableProperty(@NotNull Class<E> entity, @NotNull String propertyName) {
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
    public static <E, V> TableProperty<E, V> of(@NotNull Class<E> entity, @NotNull String propertyName) {
        return new TableProperty<>(entity, propertyName);
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

    /**
     * The database name of this field. See {@link PropertyMeta#getDbColumnName()}.
     * <p></p>
     * This is the column name which must be used in the WHERE clauses.
     * @return the database column name, not null.
     */
    @NotNull
    public String getDbColumnName() {
        return getMeta().getDbColumnName();
    }
}
