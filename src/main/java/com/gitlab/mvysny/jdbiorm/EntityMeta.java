package com.gitlab.mvysny.jdbiorm;

import org.jdbi.v3.core.annotation.Unmappable;
import org.jdbi.v3.core.mapper.Nested;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Provides meta-data for given entity.
 * <p></p>
 * Thread-safe.
 * @author mavi
 */
public final class EntityMeta {
    /**
     * usually a class implementing {@link Entity} but may be any class. Not null.
     */
    @NotNull
    public final Class<?> entityClass;

    /**
     * @param entityClass usually a class implementing {@link Entity} but may be any class. Not null.
     */
    public EntityMeta(@NotNull Class<?> entityClass) {
        this.entityClass = Objects.requireNonNull(entityClass, "entityClass");
    }

    /**
     * The name of the database table backed by this entity. Defaults to {@link Class#getSimpleName()}
     * (no conversion from `camelCase` to `hyphen_separated`)
     * but you can annotate your class with {@link Table} to override
     * that.
     *
     * @return the SQL table name, not null.
     */
    @NotNull
    public String getDatabaseTableName() {
        final Table annotation = findAnnotationRecursively(entityClass, Table.class);
        final String name = annotation == null ? null : annotation.value();
        return name == null || name.trim().isEmpty() ? entityClass.getSimpleName() : name;
    }

    /**
     * Lists all properties in this entity. Only lists persisted properties
     * (e.g. not annotated with {@link Ignore}).
     */
    @NotNull
    public Set<PropertyMeta> getProperties() {
        return Collections.unmodifiableSet(getPersistedPropertiesFor(entityClass));
    }

    /**
     * A set of database names of all persisted fields in this entity.
     * @return immutable hash set of SQL column names, not null.
     */
    @NotNull
    public Set<String> getPersistedFieldDbNames() {
        return getProperties().stream().map(it -> it.getDbColumnName()).collect(Collectors.toSet());
    }

    /**
     * Unmodifiable, thread-safe.
     */
    @Nullable
    private List<PropertyMeta> idPropertyCache;

    /**
     * The {@code id} property as declared in the entity.
     * @return usually a list with one property, but might be more in case of composite primary keys.
     */
    @NotNull
    public List<PropertyMeta> getIdProperty() {
        if (idPropertyCache == null) {
            final List<PropertyMeta> props = getProperties().stream()
                    .filter(it -> it.getNamePath().get(0).equals("id"))
                    .collect(Collectors.toList());
            if (props.isEmpty()) {
                throw new IllegalStateException("Unexpected: entity " + entityClass + " has no id field?");
            }
            idPropertyCache = Collections.unmodifiableList(new CopyOnWriteArrayList<>(props));
        }
        return idPropertyCache;
    }

    /**
     * Returns true if this entity has a composite key (the `id` field is annotated with {@link org.jdbi.v3.core.mapper.Nested}
     * and the referencing class has multiple fields).
     * @return true if this entity uses a composite key.
     */
    public boolean hasCompositeKey() {
        return getIdProperty().size() > 1;
    }

    /**
     * Returns a persisted property with given {@code propertyName} for this entity. Fails if there
     * is no such property. See {@link #getProperties()} for a list of all properties.
     * @param propertyName the Java field name, not null.
     * @throws IllegalArgumentException if there is no such property.
     */
    @NotNull
    public PropertyMeta getProperty(@NotNull String propertyName) {
        Objects.requireNonNull(propertyName, "propertyName");
        return getProperties().stream()
                .filter(it -> it.getName().equals(propertyName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("There is no such property "
                        + propertyName + " in " + entityClass + ", available fields: "
                        + getProperties().stream().map(it -> it.getName()) .collect(Collectors.joining(", "))));
    }

    private static final ConcurrentMap<Class<?>, Set<PropertyMeta>> persistedPropertiesCache =
            new ConcurrentHashMap<Class<?>, Set<PropertyMeta>>();

    private static boolean isFieldPersisted(@NotNull Field field) {
        return !Modifier.isTransient(field.getModifiers())
                && !field.isSynthetic()
                && !Modifier.isStatic(field.getModifiers())
                && !field.isAnnotationPresent(Unmappable.class)
                && !field.isAnnotationPresent(Ignore.class)
                && !field.getName().equals("Companion");  // Kotlin support
    }

    @NotNull
    private static List<Field> computePersistedFields(@NotNull Class<?> clazz) {
        if (clazz.equals(Object.class)) {
            return Collections.emptyList();
        }
        final List<Field> fields = Arrays.stream(clazz.getDeclaredFields())
                .filter(it -> isFieldPersisted(it))
                .collect(Collectors.toList());
        fields.addAll(computePersistedFields(clazz.getSuperclass()));
        return fields;
    }

    private static void visitAllPersistedFields(@NotNull Class<?> clazz,
                                                @NotNull List<Field> currentPath,
                                                @NotNull Consumer<List<Field>> discoveredFieldPathConsumer) {
        for (Field field : computePersistedFields(clazz)) {
            final ArrayList<Field> newPath = new ArrayList<>(currentPath);
            newPath.add(field);
            if (field.getAnnotation(Nested.class) != null) {
                // nested field. Need to build a chain of fields properly.
                visitAllPersistedFields(field.getType(), newPath, discoveredFieldPathConsumer);
            } else {
                discoveredFieldPathConsumer.accept(newPath);
            }
        }
    }

    /**
     * Returns the set of properties in an entity.
     */
    @NotNull
    private static Set<PropertyMeta> getPersistedPropertiesFor(@NotNull Class<?> clazz) {
        // thread-safety: this may compute the same value multiple times during high contention, this is OK
        return persistedPropertiesCache.computeIfAbsent(clazz, c -> {
                    final HashSet<PropertyMeta> metas = new HashSet<>();
                    visitAllPersistedFields(clazz, Collections.emptyList(), fields -> metas.add(new PropertyMeta(fields)));
                    return metas;
                }
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EntityMeta that = (EntityMeta) o;
        return entityClass.equals(that.entityClass);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entityClass);
    }

    private static <A extends Annotation> A findAnnotationRecursively(Class<?> entityClass, Class<A> annotationClass) {
        if (entityClass == Object.class) {
            return null;
        }
        final A annotation = entityClass.getAnnotation(annotationClass);
        if (annotation != null) {
            return annotation;
        }
        final Class<?> superclass = entityClass.getSuperclass();
        if (superclass == null) {
            return null;
        }
        return findAnnotationRecursively(superclass, annotationClass);
    }
}
