package com.gitlab.jdbiorm;

import org.jdbi.v3.core.annotation.Unmappable;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * Provides meta-data for given entity.
 *
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
        this.entityClass = Objects.requireNonNull(entityClass);
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
        final Table annotation = entityClass.getAnnotation(Table.class);
        final String name = annotation == null ? null : annotation.value();
        return name == null || name.trim().isEmpty() ? entityClass.getSimpleName() : name;
    }

    /**
     * Lists all properties in this entity. Only lists persisted properties
     * (e.g. not annotated with {@link Unmappable}).
     */
    @NotNull
    public Set<PropertyMeta> getProperties() {
        return Collections.unmodifiableSet(getPersistedPropertiesFor(entityClass));
    }

    /**
     * A list of database names of all persisted fields in this entity.
     */
    @NotNull
    public Set<String> getPersistedFieldDbNames() {
        return getProperties().stream().map(it -> it.getDbColumnName()).collect(Collectors.toSet());
    }

    /**
     * The {@code id} property as declared in the entity.
     */
    @NotNull
    public PropertyMeta getIdProperty() {
        return getProperties().stream()
                .filter(it -> it.getName().equals("id"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Unexpected: entity " + entityClass + " has no id column?"));
    }

    /**
     * Returns a persisted property with given [propertyName] for this entity. Fails if there
     * is no such property. See [properties] for a list of all properties.
     */
    @NotNull
    public PropertyMeta getProperty(@NotNull String propertyName) {
        return getProperties().stream()
                .filter(it -> it.getName().equals(propertyName))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("There is no such property "
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

    /**
     * Returns the set of properties in an entity.
     */
    @NotNull
    private static Set<PropertyMeta> getPersistedPropertiesFor(@NotNull Class<?> clazz) {
        // thread-safety: this may compute the same value multiple times during high contention, this is OK
        return persistedPropertiesCache.computeIfAbsent(clazz, c ->
                computePersistedFields(c).stream()
                        .map(it -> new PropertyMeta(it))
                        .collect(Collectors.toSet())
        );
    }
}
