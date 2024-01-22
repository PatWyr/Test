package com.gitlab.mvysny.jdbiorm;

import com.gitlab.mvysny.jdbiorm.spi.AbstractEntity;
import org.jdbi.v3.core.annotation.JdbiProperty;
import org.jdbi.v3.core.mapper.Nested;
import org.jdbi.v3.core.result.ResultBearing;
import org.jdbi.v3.core.statement.Update;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.gitlab.mvysny.jdbiorm.JdbiOrm.jdbi;

/**
 * Provides meta-data for given entity. All introspection operations execute in O(1).
 * Call {@link #of(Class)} to obtain a cached instance.
 * <p></p>
 * Thread-safe.
 * @author mavi
 */
public final class EntityMeta<E> implements Serializable {
    /**
     * Usually a class implementing {@link Entity} but may be any class. Not null.
     */
    @NotNull
    public final Class<E> entityClass;

    /**
     * Caches the <code>setId()</code> {@link Method} for given entity class. Used by {@link #setId(Object, Object)}.
     */
    @Nullable
    private final Method setIdMethod;
    @Nullable
    private final Method getIdMethod;

    @NotNull
    private final EntityProperties entityProperties;

    /**
     * Unmodifiable, thread-safe. Caches the output of {@link #getIdProperty()}.
     */
    @NotNull
    private final List<PropertyMeta> idProperty;

    /**
     * @param entityClass usually a class implementing {@link Entity} but may be any class. Not null.
     */
    private EntityMeta(@NotNull Class<E> entityClass) {
        this.entityClass = Objects.requireNonNull(entityClass, "entityClass");

        setIdMethod = Arrays.stream(entityClass.getMethods()).filter(it -> it.getName().equals("setId"))
                .findFirst().orElse(null);
        getIdMethod = Arrays.stream(entityClass.getMethods()).filter(it -> it.getName().equals("getId"))
                .findFirst().orElse(null);

        final HashSet<PropertyMeta> metas = new HashSet<>();
        visitAllPersistedFields(entityClass, Collections.emptyList(), fields -> metas.add(new PropertyMeta(entityClass, fields)));
        entityProperties = new EntityProperties(metas);

        idProperty = getProperties().stream()
                .filter(it -> it.getNamePath().get(0).equals("id")).collect(Collectors.toUnmodifiableList());

        final Table annotation = findAnnotationRecursively(entityClass, Table.class);
        final String name = annotation == null ? null : annotation.value();
        databaseTableName = name == null || name.trim().isEmpty() ? entityClass.getSimpleName() : name;
    }

    /**
     * Cached value of {@link #getDatabaseTableName()}.
     */
    @NotNull
    private final String databaseTableName;

    @NotNull
    private static final ConcurrentMap<Class<?>, EntityMeta<?>> cache =
            new ConcurrentHashMap<>();

    @NotNull
    public static <E> EntityMeta<E> of(@NotNull Class<E> entityClass) {
        Objects.requireNonNull(entityClass);
        //noinspection unchecked
        return (EntityMeta<E>) cache.computeIfAbsent(entityClass, EntityMeta::new);
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
        return databaseTableName;
    }

    /**
     * Lists all properties in this entity. Only lists persisted properties:
     * non-transient non-static fields not annotated with {@link JdbiProperty}(map = false).
     * <p></p>
     * Also includes the id property/properties.
     */
    @NotNull
    public Set<PropertyMeta> getProperties() {
        return entityProperties.getProperties();
    }

    /**
     * A set of database names of all persisted fields in this entity.
     * @return immutable hash set of SQL column names, not null.
     */
    @NotNull
    public Set<Property.DbName> getPersistedFieldDbNames() {
        return getProperties().stream().map(PropertyMeta::getDbName).collect(Collectors.toSet());
    }

    public boolean hasIdProperty() {
        return !idProperty.isEmpty();
    }

    /**
     * The {@code id} property as declared in the entity.
     * @return usually a list with one property, but might be more in case of composite primary keys.
     */
    @NotNull
    public List<PropertyMeta> getIdProperty() {
        if (!hasIdProperty()) {
            throw new IllegalStateException("Unexpected: entity " + entityClass + " has no id field");
        }
        return idProperty;
    }

    /**
     * Returns true if this entity has a composite key (the `id` field is annotated with {@link org.jdbi.v3.core.mapper.Nested}
     * and the referencing class has multiple fields).
     * @return true if this entity uses a composite key, false if this entity has no ID or just a simple ID.
     */
    public boolean hasCompositeKey() {
        return idProperty.size() > 1;
    }

    /**
     * Returns a persisted property with given {@code propertyName} for this entity. Fails if there
     * is no such property. See {@link #getProperties()} for a list of all properties.
     * @param propertyName the Java field name, not null.
     * @throws IllegalArgumentException if there is no such property.
     */
    @NotNull
    public PropertyMeta getProperty(@NotNull String propertyName) {
        return getProperty(new Property.Name(propertyName));
    }

    /**
     * Returns a persisted property with given {@code propertyName} for this entity. Fails if there
     * is no such property. See {@link #getProperties()} for a list of all properties.
     * @param propertyName the Java field name, not null.
     * @throws IllegalArgumentException if there is no such property.
     */
    @NotNull
    public PropertyMeta getProperty(@NotNull Property.Name propertyName) {
        final PropertyMeta meta = findProperty(propertyName);
        if (meta == null) {
            throw new IllegalArgumentException("There is no such property "
                    + propertyName + " in " + entityClass + ", available fields: "
                    + getProperties().stream().map(p -> p.getName().toString()).collect(Collectors.joining(", ")));
        }
        return meta;
    }

    /**
     * Returns a persisted property with given {@code propertyName} for this entity. Returns null if there
     * is no such property. See {@link #getProperties()} for a list of all properties.
     * @param propertyName the {@link PropertyMeta#getName() field name}, not null.
     * @throws IllegalArgumentException if there is no such property.
     */
    @Nullable
    public PropertyMeta findProperty(@NotNull String propertyName) {
        return findProperty(new Property.Name(propertyName));
    }

    /**
     * Returns a persisted property with given {@code propertyName} for this entity. Returns null if there
     * is no such property. See {@link #getProperties()} for a list of all properties.
     * @param propertyName the {@link PropertyMeta#getName() field name}, not null.
     * @throws IllegalArgumentException if there is no such property.
     */
    @Nullable
    public PropertyMeta findProperty(@NotNull Property.Name propertyName) {
        Objects.requireNonNull(propertyName, "propertyName");
        return entityProperties.findByName(propertyName);
    }

    /**
     * Lists all properties for an entity. Immutable, thread-safe.
     */
    private static final class EntityProperties {
        @NotNull
        private final Map<Property.Name, PropertyMeta> properties;
        @NotNull
        private final Set<PropertyMeta> set;

        public EntityProperties(@NotNull Set<PropertyMeta> p) {
            this.set = Collections.unmodifiableSet(new HashSet<>(p));
            final HashMap<Property.Name, PropertyMeta> map = new HashMap<>(set.size());
            for (PropertyMeta meta : set) {
                map.put(meta.getName(), meta);
            }
            this.properties = Collections.unmodifiableMap(map);
        }

        /**
         * @return Unmodifiable map of all properties.
         */
        @NotNull
        public Map<Property.Name, PropertyMeta> getMap() {
            return properties;
        }

        /**
         * Finds meta by {@link PropertyMeta#getName()}.
         * @param name {@link PropertyMeta#getName()}
         * @return meta or null.
         */
        @Nullable
        public PropertyMeta findByName(@NotNull Property.Name name) {
            return getMap().get(name);
        }

        @NotNull
        public Set<PropertyMeta> getProperties() {
            return set;
        }

        @Override
        public String toString() {
            return "EntityProperties{" +
                    "set=" + set +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            EntityProperties that = (EntityProperties) o;
            return set.equals(that.set);
        }

        @Override
        public int hashCode() {
            return Objects.hash(set);
        }
    }

    private static boolean isJdbiPropertyMap(@NotNull Field field) {
        final JdbiProperty a = field.getAnnotation(JdbiProperty.class);
        return a == null || a.map();
    }

    private static boolean isFieldPersisted(@NotNull Field field) {
        return !Modifier.isTransient(field.getModifiers())
                && !field.isSynthetic()
                && !Modifier.isStatic(field.getModifiers())
                && isJdbiPropertyMap(field)
                && !field.isAnnotationPresent(Ignore.class)
                && !field.getName().equals("Companion");  // Kotlin support
    }

    @NotNull
    private static List<Field> computePersistedFields(@NotNull Class<?> clazz) {
        if (clazz.equals(Object.class)) {
            return Collections.emptyList();
        }
        final List<Field> fields = Arrays.stream(clazz.getDeclaredFields())
                .filter(EntityMeta::isFieldPersisted)
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EntityMeta<?> that = (EntityMeta<?>) o;
        return entityClass.equals(that.entityClass);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entityClass);
    }

    @Nullable
    private static <A extends Annotation> A findAnnotationRecursively(@NotNull Class<?> entityClass, @NotNull Class<A> annotationClass) {
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
            return entityClass.getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException |
                 InvocationTargetException | NoSuchMethodException e) {
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

    /**
     * Calls {@code AbstractEntity#setId} on given entity.
     * @param entity the entity of type E, not null.
     * @param id the ID, may be null.
     */
    public void setId(@NotNull Object entity, @Nullable Object id) {
        try {
            if (setIdMethod == null) {
                throw new IllegalStateException("Invalid state: setId() not found on " + entityClass);
            }
            setIdMethod.invoke(entity, id);
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
            if (getIdMethod == null) {
                throw new IllegalStateException("Invalid state: getId() not found on " + entityClass);
            }
            return getIdMethod.invoke(entity);
        } catch (IllegalAccessException | InvocationTargetException e) {
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
                    .define("FIELDS", properties.stream().map(p -> p.getDbName().getUnqualifiedName()).collect(Collectors.joining(", ")))
                    .define("FIELD_VALUES", properties.stream().map(it -> ":" + it.getName()).collect(Collectors.joining(", ")));
            for (PropertyMeta property : properties) {
                update.bind(property.getName().getName(), property.get(entity));
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
                        .executeAndReturnGeneratedKeys(idProperty.getDbName().getUnqualifiedName());
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
                    .define("FIELDS", properties.stream().map(it -> it.getDbName().getUnqualifiedName() + " = :" + it.getDbName().getUnqualifiedName()).collect(Collectors.joining(", ")))
                    .define("ID", idProperties.stream().map(it -> it.getDbName().getUnqualifiedName() + " = :" + it.getDbName().getUnqualifiedName()).collect(Collectors.joining(" AND ")));
            for (PropertyMeta property : properties) {
                update.bind(property.getDbName().getUnqualifiedName(), property.get(entity));
            }
            for (PropertyMeta idProperty : idProperties) {
                update.bind(idProperty.getDbName().getUnqualifiedName(), idProperty.get(entity));
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

    // see https://www.digitalocean.com/community/tutorials/serialization-in-java#serialization-proxy-pattern
    private static class SerializationProxy implements Serializable {
        @NotNull
        private final Class<?> clazz;

        public SerializationProxy(@NotNull Class<?> clazz) {
            this.clazz = clazz;
        }

        private Object readResolve() {
            return EntityMeta.of(clazz);
        }
    }

    @NotNull
    private Object writeReplace() {
        return new SerializationProxy(entityClass);
    }

    private void readObject(@NotNull ObjectInputStream ois) throws InvalidObjectException {
        throw new InvalidObjectException("Proxy is not used, something fishy");
    }
}
