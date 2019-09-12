package com.gitlab.jdbiorm;

import java.util.Objects;

/**
 * Provides meta-data for given entity.
 *
 * @author mavi
 */
public final class EntityMeta {
    /**
     * usually a class implementing {@link Entity} but may be any class.
     */
    public final Class<?> entityClass;

    /**
     * @param entityClass usually a class implementing {@link Entity} but may be any class.
     */
    public EntityMeta(Class<?> entityClass) {
        this.entityClass = Objects.requireNonNull(entityClass);
    }

    /**
     * The name of the database table backed by this entity. Defaults to [Class.getSimpleName]
     * (no conversion from `camelCase` to `hyphen_separated`) but you can annotate your class with [Table.dbname] to override
     * that.
     */
    public String getDatabaseTableName() {
        final Table annotation = entityClass.getAnnotation(Table.class);
        final String name = annotation == null ? null : annotation.value();
        return name == null || name.trim().isEmpty() ? entityClass.getSimpleName() : name;
    }
}
