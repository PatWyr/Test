package com.gitlab.mvysny.jdbiorm;

import org.jdbi.v3.core.statement.Query;
import org.jdbi.v3.core.statement.SqlStatement;
import org.jdbi.v3.core.statement.Update;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.gitlab.mvysny.jdbiorm.JdbiOrm.jdbi;

/**
 * Data access object, provides instances of given {@link Entity}.
 * To use, just add a static field to your entity as follows:
 * <pre>
 * public class Person implements Entity<Long> {
 *   public static final Dao&lt;Person, Long> dao = new Dao<>(Person.class);
 * }
 * </pre>
 * You can now use `Person.dao.findAll()`, `Person.dao.get(25)` and other nice methods :)
 *
 * @param <T> the type of the {@link Entity} provided by this Dao
 * @param <ID> the type of {@link Entity} ID.
 */
public class Dao<T extends AbstractEntity<ID>, ID> extends DaoOfAny<T> {
    public Dao(@NotNull Class<T> entityClass) {
        super(entityClass);
    }

    /**
     * Retrieves entity with given {@code id}. Fails if there is no such entity.
     *
     * @throws IllegalStateException if there is no entity with given id.
     */
    @NotNull
    public T getById(@NotNull ID id) {
        Objects.requireNonNull(id, "id");
        final T result = findById(id);
        if (result == null) {
            throw new IllegalStateException("There is no " + entityClass.getSimpleName() + " for id " + id);
        }
        return result;
    }

    /**
     * Retrieves entity with given {@code id}. Returns null if there is no such entity.
     */
    @Nullable
    public T findById(@NotNull ID id) {
        Objects.requireNonNull(id, "id");
        return jdbi().withHandle(handle -> {
            final Query query = handle.createQuery("select <FIELDS> from <TABLE> where <ID>")
                    .define("FIELDS", String.join(", ", meta.getPersistedFieldDbNames()))
                    .define("TABLE", meta.getDatabaseTableName());
            passIdValuesToQuery(query, id);
            return query.map(getRowMapper())
                    .findFirst().orElse(null);
        });
    }

    /**
     * Computes the WHERE clause (without the `WHERE` keyword) and defines it into the query
     * under the `ID` key. Also retrieves all values from the (potentially composite) id and
     * binds it into the query.
     * @param query the query to modify, not null.
     * @param id the ID value, not null.
     */
    public void passIdValuesToQuery(@NotNull SqlStatement<?> query, @NotNull ID id) {
        Objects.requireNonNull(id, "id");
        final List<PropertyMeta> idProperties = meta.getIdProperty();
        query.define("ID", idProperties.stream().map(it -> it.getDbColumnName() + " = :" + it.getDbColumnName()).collect(Collectors.joining(" AND ")));

        if (meta.hasCompositeKey()) {
            // in order to be able to call PropertyMeta.get() we need to pass in the Entity instance, not the ID instance.
            // so we'll use a little trick...
            final T entity = meta.newEntityInstance();
            meta.setId(entity, id);
            for (PropertyMeta idProperty : idProperties) {
                query.bind(idProperty.getDbColumnName(), idProperty.get(entity));
            }
        } else {
            // fall back to the safer+faster simple way
            query.bind(idProperties.get(0).getDbColumnName(), id);
        }
    }

    /**
     * Checks whether there exists any row with given id.
     */
    public boolean existsById(@NotNull ID id) {
        Objects.requireNonNull(id, "id");
        return jdbi().withHandle(handle -> {
            final Query query = handle.createQuery("select count(1) from <TABLE> where <ID>")
                    .define("TABLE", meta.getDatabaseTableName());
            passIdValuesToQuery(query, id);
            return query.mapTo(Long.class).one() > 0;
        });
    }

    /**
     * Deletes row with given ID. Does nothing if there is no such row.
     */
    public void deleteById(@NotNull ID id) {
        Objects.requireNonNull(id, "id");
        jdbi().withHandle(handle -> {
            final Update update = handle.createUpdate("delete from <TABLE> where <ID>")
                    .define("TABLE", meta.getDatabaseTableName());
            passIdValuesToQuery(update, id);
            return update.execute();
        });
    }
}
