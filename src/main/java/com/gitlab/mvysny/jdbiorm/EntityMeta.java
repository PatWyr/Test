package com.gitlab.mvysny.jdbiorm;

import com.gitlab.mvysny.jdbiorm.spi.AbstractEntity;
import org.jdbi.v3.core.annotation.Unmappable;
import org.jdbi.v3.core.mapper.Nested;
import org.jdbi.v3.core.result.ResultBearing;
import org.jdbi.v3.core.statement.Update;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.gitlab.mvysny.jdbiorm.JdbiOrm.jdbi;

/**
 * Provides meta-data for given entity.
 * <p></p>
 * Thread-safe.
 * @author mavi
 */
public final class EntityMeta<E> implements Serializable {
    /**
     * usually a class implementing {@link Entity} but may be any class. Not null.
     */
    @NotNull
    public final Class<E> entityClass;

    /**
     * @param entityClass usually a class implementing {@link Entity} but may be any class. Not null.
     */
    public EntityMeta(@NotNull Class<E> entityClass) {
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
     * Lists all properties in this entity. Only lists persisted properties:
     * non-transient non-static fields not annotated with {@link Ignore}.
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
    private transient volatile List<PropertyMeta> idPropertyCache;

    /**
     * The {@code id} property as declared in the entity.
     * @return usually a list with one property, but might be more in case of composite primary keys.
     */
    @NotNull
    public List<PropertyMeta> getIdProperty() {
        List<PropertyMeta> cache = idPropertyCache;
        if (cache == null) {
            final List<PropertyMeta> props = getProperties().stream()
                    .filter(it -> it.getNamePath().get(0).equals("id"))
                    .collect(Collectors.toList());
            if (props.isEmpty()) {
                throw new IllegalStateException("Unexpected: entity " + entityClass + " has no id field?");
            }
            cache = Collections.unmodifiableList(new CopyOnWriteArrayList<>(props));
            idPropertyCache = cache;
        }
        return cache;
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

    /**
     * Copies all values of all persisted fields from {@code sourceEntity} to {@code targetEntity}.
     * @param sourceEntity the entity to copy the values from, not null.
     * @param targetEntity the entity to copy the values to, not null.
     */
    public void copyTo(@NotNull E sourceEntity, @NotNull E targetEntity) {
        Objects.requireNonNull(sourceEntity);
        Objects.requireNonNull(targetEntity);
        for (PropertyMeta property : getProperties()) {
            final Object value = property.get(sourceEntity);
            property.set(targetEntity, value);
        }
    }

    /**
     * Creates new empty instance of the entity.
     * @return the new instance, not null.
     */
    @NotNull
    public E newEntityInstance() {
        try {
            return entityClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Uses {@link #newEntityInstance()} and {@link #copyTo(Object, Object)}
     * to clone given entity.
     * @param source the entity to clone, not null.
     * @return a clone, not null.
     */
    @NotNull
    public E clone(@NotNull E source) {
        Objects.requireNonNull(source);
        final E result = newEntityInstance();
        copyTo(source, result);
        return result;
    }

    private static final ConcurrentMap<Class<?>, Method> getIdCache = new ConcurrentHashMap<>();

    /**
     * Calls {@code AbstractEntity#setId} on given entity.
     * @param entity the entity of type E, not null.
     * @param id the ID, may be null.
     */
    public void setId(@NotNull Object entity, @Nullable Object id) {
        final Method setId = getIdCache.computeIfAbsent(entityClass, new Function<Class<?>, Method>() {
            @Override
            public Method apply(Class<?> aClass) {
                final Method setId = Arrays.stream(aClass.getMethods()).filter(it -> it.getName().equals("setId"))
                        .findFirst().orElse(null);
                if (setId == null) {
                    throw new IllegalStateException("Invalid state: setId() not found on " + entityClass);
                }
                return setId;
            }
        });
        try {
            setId.invoke(entity, id);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Calls {@code AbstractEntity#getId} on given entity.
     * @param entity the entity of type E, not null.
     * @return the ID, may be null.
     */
    @Nullable
    public Object getId(@NotNull Object entity) {
        try {
            final Method getId = entityClass.getMethod("getId");
            return getId.invoke(entity);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * The default implementation of Entity.validate()
     * @param entity the entity of type E
     */
    public void defaultValidate(@NotNull final Object entity) {
        Objects.requireNonNull(entity);
        final Set<ConstraintViolation<Object>> violations = JdbiOrm.getValidator().validate(entity);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }

    /**
     * The default implementation of Entity.create()
     * @param entity the entity of type E
     */
    public void defaultCreate(@NotNull final Object entity) {
        Objects.requireNonNull(entity);
        jdbi().useHandle(handle -> {
            final List<PropertyMeta> properties = new ArrayList<>(getProperties());
            final List<PropertyMeta> idProperties = getIdProperty();
            if (getId(entity) == null) {
                // the ID is auto-generated by the database, do not include it in the INSERT statement.
                properties.removeAll(idProperties);
            }
            final Update update = handle.createUpdate("insert into <TABLE> (<FIELDS>) values (<FIELD_VALUES>)")
                    .define("TABLE", getDatabaseTableName())
                    .define("FIELDS", properties.stream().map(it -> it.getDbColumnName()).collect(Collectors.joining(", ")))
                    .define("FIELD_VALUES", properties.stream().map(it -> ":" + it.getName()).collect(Collectors.joining(", ")));
            for (PropertyMeta property : properties) {
                update.bind(property.getName(), property.get(entity));
            }
            if (idProperties.size() > 1) {
                if (getId(entity) == null) {
                    // we don't support retrieving generated keys for composite PKs at the moment...
                    throw new UnsupportedOperationException("we don't support retrieving generated keys for composite PKs at the moment...");
                }
                update.execute();
            } else {
                final PropertyMeta idProperty = idProperties.get(0);
                final ResultBearing resultBearing = update
                        .executeAndReturnGeneratedKeys(idProperty.getDbColumnName());
                final Object generatedKey = resultBearing
                        .mapTo(idProperty.getValueType())
                        .findFirst().orElse(null);
                if (getId(entity) == null) {
                    Objects.requireNonNull(generatedKey, "The database have returned null key for the created record. Have you used AUTO INCREMENT or SERIAL for primary key?");
                    setId(entity, idProperty.getValueType().cast(generatedKey));
                }
            }
        });
    }

    /**
     * The default implementation of Entity.save()
     * @param entity the entity of type E
     */
    public void defaultSave(@NotNull final Object entity) {
        Objects.requireNonNull(entity);
        jdbi().useHandle(handle -> {
            List<PropertyMeta> properties = new ArrayList<>(getProperties());
            final List<PropertyMeta> idProperties = getIdProperty();
            properties.removeAll(idProperties);

            // build the Statement
            final Update update = handle.createUpdate("update <TABLE> set <FIELDS> where <ID>")
                    .define("TABLE", getDatabaseTableName())
                    .define("FIELDS", properties.stream().map(it -> it.getDbColumnName() + " = :" + it.getDbColumnName()).collect(Collectors.joining(", ")))
                    .define("ID", idProperties.stream().map(it -> it.getDbColumnName() + " = :" + it.getDbColumnName()).collect(Collectors.joining(" AND ")));
            for (PropertyMeta property : properties) {
                update.bind(property.getDbColumnName(), property.get(entity));
            }
            for (PropertyMeta idProperty : idProperties) {
                update.bind(idProperty.getDbColumnName(), idProperty.get(entity));
            }

            // execute the Statement
            final int result = update
                    .bindBean(entity)
                    .execute();
            if (result != 1) {
                throw new IllegalStateException("We expected to update only one row but we updated "
                        + result + " - perhaps there is no row with id " + getId(entity) + "?");
            }
        });

    }

    /**
     * The default implementation of Entity.reload()
     * @param entity the entity of type E
     */
    @SuppressWarnings("unchecked")
    public void defaultReload(@NotNull Object entity) {
        final Dao dao = new Dao<>(((Class<AbstractEntity>) entityClass));
        final AbstractEntity<?> current = dao.getById(getId(entity));
        dao.meta.copyTo(current, entity);
    }
}
