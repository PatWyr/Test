package com.gitlab.mvysny.jdbiorm;

import org.jdbi.v3.core.mapper.Nested;
import org.jdbi.v3.core.mapper.reflect.ColumnName;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Provides meta-data for a particular property of a particular {@link Entity}.
 * <p></p>
 * Thread-safe.
 */
public final class PropertyMeta {

    /**
     * The Java reflection field, used to read the value of this property.
     * Usually this will be 1-sized list of a single Field reading a field straight
     * from given entity.
     * <p></p>
     * In case of {@link Nested}-annotated fields, this is a chain of fields
     * used to obtain the final property value. The chain starts with an Entity field,
     * and iterates over {@link Nested} annotations until a terminal property is reached.
     * <p></p>
     * The field path is most useful for composite primary keys, but it may have
     * other interesting use-cases.
     * <p></p>
     * Must not be empty nor null.
     */
    @NotNull
    private final LinkedList<Field> fieldPath;

    /**
     * Computed from {@link #fieldPath}. Unmodifiable.
     */
    @NotNull
    private final List<String> namePath;

    /**
     * Derived from {@link #namePath}.
     */
    @NotNull
    private final Property.Name name;

    /**
     * Creates the property.
     * @param fieldPath the field, not null. See {@link #fieldPath} for more info.
     */
    public PropertyMeta(@NotNull List<Field> fieldPath) {
        this.fieldPath = new LinkedList<>(Objects.requireNonNull(fieldPath, "fieldPath"));
        if (this.fieldPath.isEmpty()) {
            throw new IllegalArgumentException("Parameter fieldPath: invalid value " + fieldPath + ": must not be empty");
        }
        namePath = Collections.unmodifiableList(fieldPath.stream().map(Field::getName).collect(Collectors.toList()));
        name = new Property.Name(namePath.size() == 1 ? namePath.get(0) : String.join(".", namePath));
    }

    /**
     * Wraps given Java reflection field as an entity property.
     * @param field the field, not null.
     */
    public PropertyMeta(@NotNull Field field) {
        this(Collections.singletonList(Objects.requireNonNull(field, "field")));
    }

    /**
     * The name of the last {@link Field} in the field path.
     *
     * @return The {@link Field#getName()}, not null.
     */
    @NotNull
    public String getLastName() {
        return fieldPath.getLast().getName();
    }

    /**
     * The full name of this property, most often the {@link Field#getName() Java field name} from some entity bean.
     * However, in case of composite IDs this is a comma-separated {@link #getNamePath()} e.g. "id.component1".
     * @return The {@link Field#getName()}, not null.
     */
    @NotNull
    public Property.Name getName() {
        return name;
    }

    /**
     * {@link Field#getName() field names} of {@link #fieldPath}.
     * Usually of size 1, but may be longer in case of composite IDs.
     * @return the
     */
    @NotNull
    public List<String> getNamePath() {
        return namePath;
    }

    /**
     * The database name of given field. Defaults to {@link #getLastName()}, but
     * it can be changed via the {@link ColumnName} annotation.
     * <p></p>
     * This is the column name which must be used in the WHERE clauses.
     * @return the database column name, not null.
     */
    @NotNull
    public String getDbColumnName() {
        final ColumnName annotation = fieldPath.getLast().getAnnotation(ColumnName.class);
        return annotation == null ? getLastName() : annotation.value();
    }

    /**
     * The type of the value this field can take.
     * @return the value type, not null.
     */
    @NotNull
    public Class<?> getValueType() {
        return fieldPath.getLast().getType();
    }

    @Nullable
    public Object get(@NotNull Object entity) {
        Objects.requireNonNull(entity, "entity");
        Object current = entity;
        for (Field field : fieldPath) {
            field.setAccessible(true);
            try {
                current = field.get(current);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
            if (current == null) {
                return null;
            }
        }
        return current;
    }

    @Override
    public String toString() {
        final String path = fieldPath.stream().skip(1).map(Field::getName).collect(Collectors.joining("/"));
        return "PropertyMeta{" + fieldPath.getFirst() + "/" + path + ", dbColumnName=" + getDbColumnName() + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PropertyMeta that = (PropertyMeta) o;
        return fieldPath.equals(that.fieldPath);
    }

    @Override
    public int hashCode() {
        return fieldPath.hashCode();
    }

    /**
     * Sets the value of this property for given entity. In case of nested entities,
     * any null field along the {@link #fieldPath} is automatically set to a non-null value
     * by invoking the default constructor.
     * @param entity the entity, not null.
     * @param value the new value, may be null.
     */
    public void set(@NotNull Object entity, @Nullable Object value) {
        Object current = entity;
        for (Field field : fieldPath.subList(0, fieldPath.size() - 1)) {
            field.setAccessible(true);
            try {
                Object newCurrent = field.get(current);
                if (newCurrent == null) {
                    newCurrent = field.getType().newInstance();
                    field.set(current, newCurrent);
                }
                current = newCurrent;
            } catch (IllegalAccessException | InstantiationException e) {
                throw new RuntimeException(e);
            }
        }
        try {
            fieldPath.get(fieldPath.size() - 1).set(current, value);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
