package com.gitlab.mvysny.jdbiorm;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

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
public class Dao<T extends Entity<ID>, ID> extends DaoOfAny<T> {
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
        return jdbi().withHandle(handle -> handle.createQuery("select * from <TABLE> where <ID> = :id")
                .define("TABLE", meta.getDatabaseTableName())
                .define("ID", meta.getIdProperty().getDbColumnName())
                .bind("id", id)
                .map(getRowMapper())
                .findFirst().orElse(null)
        );
    }

    /**
     * Checks whether there exists any row with given id.
     */
    public boolean existsById(@NotNull ID id) {
        Objects.requireNonNull(id, "id");
        return jdbi().withHandle(handle -> handle.createQuery("select count(1) from <TABLE> where <ID> = :id")
                .define("TABLE", meta.getDatabaseTableName())
                .define("ID", meta.getIdProperty().getDbColumnName())
                .bind("id", id)
                .mapTo(Long.class).one() > 0);
    }

    /**
     * Deletes row with given ID. Does nothing if there is no such row.
     */
    public void deleteById(@NotNull ID id) {
        Objects.requireNonNull(id, "id");
        jdbi().withHandle(handle -> handle.createUpdate("delete from <TABLE> where <ID>=:id")
                .define("TABLE", meta.getDatabaseTableName())
                .define("ID", meta.getIdProperty().getDbColumnName())
                .bind("id", id)
                .execute());
    }
}
