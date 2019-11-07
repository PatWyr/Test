package com.gitlab.mvysny.jdbiorm;

import org.jdbi.v3.core.mapper.reflect.ColumnName;
import org.jdbi.v3.core.result.ResultBearing;
import org.jdbi.v3.core.statement.Update;
import org.jetbrains.annotations.Nullable;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

import static com.gitlab.mvysny.jdbiorm.JdbiOrm.jdbi;

/**
 * Allows you to fetch rows of a database table, and adds useful utility methods {@link #save()}
 * and {@link #delete()}.
 * <p>
 * Automatically will try to store/update/retrieve all non-transient fields declared by this class and all superclasses.
 * To exclude fields, either mark them {@code transient} or {@link org.jdbi.v3.core.annotation.Unmappable}.
 * <p>
 * If your table has no primary key or there is other reason you don't want to use this interface, you can still use
 * the DAO methods (see {@link DaoOfAny} for more details); you only lose the ability to {@link #save()},
 * {@link #create()} and {@link #delete()}.
 * <h3>Mapping columns</h3>
 * Use the {@link ColumnName} annotation to change the name of the column.
 * <h3>Auto-generated IDs vs pre-provided IDs</h3>
 * There are generally three cases for entity ID generation:
 * <ul>
 * <li>IDs generated by the database when the `INSERT` statement is executed</li>
 * <li>Natural IDs, such as a NaturalPerson with ID pre-provided by the government (social security number etc).</li>
 * <li>IDs created by the application, for example via {@link UUID#randomUUID()}</li>
 * </ul>
 * The {@link #save()} method is designed to work out-of-the-box only for the first case (IDs auto-generated by the database). In this
 * case, {@link #save()} emits `INSERT` when the ID is null, and `UPDATE` when the ID is not null.
 * <p>
 * When the ID is pre-provided, you can only use {@link #save()} method to update a row in the database; using {@link #save()} to create a
 * row in the database will throw an exception. In order to create an
 * entity with a pre-provided ID, you need to use the {@link #create()} method:
 * <pre>
 * new NaturalPerson("12345678", "Albedo").create()
 * </pre>
 * <p>
 * For entities with IDs created by the application you can make {@link #save(boolean)} work properly, by overriding the {@link #create(boolean)} method
 * as follows:
 * <pre>
 * public void create(boolean validate) {
 *   id = UUID.randomUUID()
 *   Entity.super.create(validate)
 * }
 * </pre>
 *
 * @param <ID> the type of the primary key. All finder methods will only accept this type of ids.
 * @author mavi
 */
public interface Entity<ID> extends Serializable {
    /**
     * The ID primary key. You can use the {@link org.jdbi.v3.core.mapper.reflect.ColumnName} annotation to change the actual db column name.
     *
     * @return the ID primary key, may be null.
     */
    @Nullable
    ID getId();

    /**
     * Sets the ID key.
     *
     * @param id the ID primary key, may be null.
     */
    void setId(@Nullable ID id);

    /**
     * Validates current entity. By default performs the java validation: just add {@code javax.validation}
     * annotations to entity properties.
     * <p></p>
     * Make sure to add the validation annotations to
     * fields otherwise they will be ignored.
     * <p></p>
     * You can override this method to perform additional validations on the level of the entire entity.
     *
     * @throws javax.validation.ValidationException when validation fails.
     */
    default void validate() {
        final Set<ConstraintViolation<Entity<ID>>> violations = JdbiOrm.getValidator().validate(this);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }

    default boolean isValid() {
        try {
            validate();
            return true;
        } catch (ConstraintViolationException ex) {
            return false;
        }
    }

    /**
     * Deletes this entity from the database. Fails if {@link #getId()} is null,
     * since it is expected that the entity is already in the database.
     */
    default void delete() {
        if (getId() == null) {
            throw new IllegalStateException("The id is null, the entity is not yet in the database");
        }
        //noinspection unchecked
        new Dao<>(getClass()).deleteById(getId());
    }

    /**
     * Always issues the database `INSERT`, even if the {@link #getId()} is not null. This is useful for two cases:
     * <ul><li>When the entity has a natural ID, such as a NaturalPerson with ID pre-provided by the government (social security number etc),</li>
     * <li>ID auto-generated by the application, e.g. UUID</li></ul>
     * <p></p>
     * It is possible to use this function with entities with IDs auto-generated by the database, but it may be simpler to
     * simply use {@link #save()}.
     */
    default void create(boolean validate) {
        if (validate) {
            validate();
        }
        jdbi().useHandle(handle -> {
            final EntityMeta meta = new EntityMeta(getClass());
            final List<PropertyMeta> properties = new ArrayList<>(meta.getProperties());
            final PropertyMeta idProperty = meta.getIdProperty();
            if (getId() == null) {
                // the ID is auto-generated by the database, do not include it in the INSERT statement.
                properties.remove(idProperty);
            }
            final Update update = handle.createUpdate("insert into <TABLE> (<FIELDS>) values (<FIELD_VALUES>)")
                    .define("TABLE", meta.getDatabaseTableName())
                    .define("FIELDS", properties.stream().map(it -> it.getDbColumnName()).collect(Collectors.joining(", ")))
                    .define("FIELD_VALUES", properties.stream().map(it -> ":" + it.getName()).collect(Collectors.joining(", ")));
            for (PropertyMeta property : properties) {
                update.bind(property.getName(), property.get(this));
            }
            final ResultBearing resultBearing = update
                    .executeAndReturnGeneratedKeys(idProperty.getDbColumnName());
            final Object generatedKey = resultBearing
                    .mapTo(idProperty.getValueType())
                    .findFirst().orElse(null);
            if (getId() == null) {
                Objects.requireNonNull(generatedKey, "The database have returned null key for the created record. Have you used AUTO INCREMENT or SERIAL for primary key?");
                //noinspection unchecked
                setId((ID) idProperty.getValueType().cast(generatedKey));
            }
        });
    }

    /**
     * Always issues the database `INSERT`, even if the {@link #getId()} is not null. This is useful for two cases:
     * <ul><li>When the entity has a natural ID, such as a NaturalPerson with ID pre-provided by the government (social security number etc),</li>
     * <li>ID auto-generated by the application, e.g. UUID</li></ul>
     * <p></p>
     * It is possible to use this function with entities with IDs auto-generated by the database, but it may be simpler to
     * simply use {@link #save()}.
     */
    default void create() {
        create(true);
    }

    /**
     * Creates a new row in a database (if {@link #getId()} is null) or updates the row in a database (if {@link #getId()} is not null).
     * When creating, this method simply calls the {@link #create(boolean)} method.
     * <p></p>
     * It is expected that the database will generate an id for us (by sequences,
     * `auto_increment` or other means). That generated ID is then automatically stored into the {@link #getId()} field.
     * <p></p>
     * The bean is validated first, by calling {@link #validate()}.
     * You can bypass this by setting the {@code validate} parameter to false, but that's not
     * recommended.
     * <p></p>
     * <strong>WARNING</strong>: if your entity has pre-provided (natural) IDs, you must not call
     * this method with the intent to insert the entity into the database - this method will always run UPDATE and then
     * fail (since nothing has been updated since the row is not in the database yet).
     * To force create the database row, call {@link #create()}.
     * <p></p>
     * <strong>INFO</strong>>: Entities with IDs created by the application can be made to work properly, by overriding {@link #create()}
     * and {@link #create(boolean)} method accordingly. See {@link Entity} doc for more details.
     *
     * @throws IllegalStateException if the database didn't provide a new ID (upon new row creation),
     *                               or if there was no row (if {@link #getId()} was not null).
     */
    default void save(boolean validate) {
        if (validate) {
            validate();
        }
        jdbi().useHandle(handle -> {
            final EntityMeta meta = new EntityMeta(getClass());
            if (getId() == null) {
                create(false);  // no need to validate again
            } else {
                List<PropertyMeta> properties = new ArrayList<>(meta.getProperties());
                final PropertyMeta idProperty = meta.getIdProperty();
                properties.remove(idProperty);
                final Update update = handle.createUpdate("update <TABLE> set <FIELDS> where <ID> = :<ID>")
                        .define("TABLE", meta.getDatabaseTableName())
                        .define("FIELDS", properties.stream().map(it -> it.getDbColumnName() + " = :" + it.getDbColumnName()).collect(Collectors.joining(", ")))
                        .define("ID", idProperty.getDbColumnName());
                for (PropertyMeta property : properties) {
                    update.bind(property.getDbColumnName(), property.get(this));
                }
                update.bind(idProperty.getDbColumnName(), idProperty.get(this));
                final int result = update
                        .bindBean(this)
                        .execute();
                if (result != 1) {
                    throw new IllegalStateException("We expected to update only one row but we updated "
                            + result + " - perhaps there is no row with id " + getId() + "?");
                }
            }
        });
    }

    /**
     * Creates a new row in a database (if {@link #getId()} is null) or updates the row in a database (if {@link #getId()} is not null).
     * When creating, this method simply calls the {@link #create(boolean)} method.
     * <p></p>
     * It is expected that the database will generate an id for us (by sequences,
     * `auto_increment` or other means). That generated ID is then automatically stored into the {@link #getId()} field.
     * <p></p>
     * The bean is validated first, by calling {@link #validate()}.
     * You can bypass this, by calling {@link #save(boolean)} and setting the {@code validate} parameter to false, but that's not
     * recommended.
     * <p></p>
     * <strong>WARNING</strong>: if your entity has pre-provided (natural) IDs, you must not call
     * this method with the intent to insert the entity into the database - this method will always run UPDATE and then
     * fail (since nothing has been updated since the row is not in the database yet).
     * To force create the database row, call {@link #create()}.
     * <p></p>
     * <strong>INFO</strong>>: Entities with IDs created by the application can be made to work properly, by overriding {@link #create()}
     * and {@link #create(boolean)} method accordingly. See {@link Entity} doc for more details.
     *
     * @throws IllegalStateException if the database didn't provide a new ID (upon new row creation),
     *                               or if there was no row (if {@link #getId()} was not null).
     */
    default void save() {
        save(true);
    }
}
