package com.gitlab.mvysny.jdbiorm;

import org.jdbi.v3.core.mapper.reflect.ColumnName;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.Objects;

/**
 * Provides meta-data for a particular property of a particular {@link Entity}.
 */
public final class PropertyMeta {

    @NotNull
    public final Field field;

    /**
     * The backing field.
     * @param field the field, not null.
     */
    public PropertyMeta(@NotNull Field field) {
        this.field = Objects.requireNonNull(field);
    }

    /**
     * The {@link Field#getName()}
     *
     * @return The {@link Field#getName()}, not null.
     */
    @NotNull
    public String getName() {
        return field.getName();
    }

    /**
     * The database name of given field. Defaults to {@link #getName()}, but
     * it can be changed via the {@link ColumnName} annotation.
     * <p></p>
     * This is the column name which must be used in the WHERE clauses.
     * @return the database column name, not null.
     */
    @NotNull
    public String getDbColumnName() {
        final ColumnName annotation = field.getAnnotation(ColumnName.class);
        return annotation == null ? getName() : annotation.value();
    }

    /**
     * The type of the value this field can take.
     * @return the value type, not null.
     */
    @NotNull
    public Class<?> getValueType() {
        return field.getType();
    }

    @Nullable
    public Object get(@NotNull Object entity) {
        field.setAccessible(true);
        try {
            return field.get(entity);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        return "PropertyMeta{" + field + ", dbColumnName=" + getDbColumnName() + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PropertyMeta that = (PropertyMeta) o;
        return field.equals(that.field);
    }

    @Override
    public int hashCode() {
        return Objects.hash(field);
    }
}
