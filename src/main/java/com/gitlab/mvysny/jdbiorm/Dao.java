package com.gitlab.mvysny.jdbiorm;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.mapper.reflect.BeanMapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
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
 * @param <T> the type of the [Entity] provided by this Dao
 */
public class Dao<T extends Entity<ID>, ID> {
    @NotNull
    protected final Class<T> entityClass;
    @NotNull
    protected final EntityMeta meta;

    public Dao(@NotNull Class<T> entityClass) {
        this.entityClass = Objects.requireNonNull(entityClass);
        meta = new EntityMeta(entityClass);
    }

    @NotNull
    protected RowMapper<T> getRowMapper() {
        return BeanMapper.of(entityClass);
    }

    /**
     * Finds all rows in given table. Fails if there is no table in the database with the
     * name of {@link EntityMeta#getDatabaseTableName()}. The list is eager
     * and thus it's useful for small-ish tables only.
     */
    @NotNull
    public List<T> findAll() {
        return jdbi().withHandle(handle -> handle.createQuery("select * from <TABLE>")
                .define("TABLE", meta.getDatabaseTableName())
                .map(getRowMapper())
                .list()
        );
    }

    /**
     * Retrieves entity with given {@code id}. Fails if there is no such entity. See [Dao] on how to add this to your entities.
     *
     * @throws IllegalArgumentException if there is no entity with given id.
     */
    @NotNull
    public T getById(@NotNull ID id) {
        return Objects.requireNonNull(findById(id), () ->
                "There is no " + entityClass.getSimpleName() + " for id " + id);
    }

    /**
     * Retrieves entity with given {@code id}. Returns null if there is no such entity.
     */
    @Nullable
    public T findById(@NotNull ID id) {
        return jdbi().withHandle(handle -> handle.createQuery("select * from <TABLE> where <ID> = :id")
                .define("TABLE", meta.getDatabaseTableName())
                .define("ID", meta.getIdProperty().getDbColumnName())
                .bind("id", id)
                .map(getRowMapper())
                .findFirst().orElse(null)
        );
    }

    /**
     * Deletes all rows from this database table.
     */
    public void deleteAll() {
        jdbi().useHandle(handle -> handle.createUpdate("delete from <TABLE>")
                .define("TABLE", meta.getDatabaseTableName())
                .execute());
    }

    /**
     * Counts all rows in this table.
     */
    public long getCount() {
        return jdbi().withHandle(handle -> handle.createQuery("select count(*) from <TABLE>")
                .define("TABLE", meta.getDatabaseTableName())
                .mapTo(Long.class).one());
    }

    /**
     * Checks whether there exists any instance of [clazz].
     */
    public boolean existsAny() {
        return jdbi().withHandle(handle -> handle.createQuery("select count(1) from <TABLE>")
                .define("TABLE", meta.getDatabaseTableName())
                .mapTo(Long.class).one() > 0);
    }

    /**
     * Checks whether there exists any instance of [clazz] with given id.
     */
    public boolean existsById(@NotNull ID id) {
        return jdbi().withHandle(handle -> handle.createQuery("select count(1) from <TABLE> where <ID> = :id")
                .define("TABLE", meta.getDatabaseTableName())
                .define("ID", meta.getIdProperty().getDbColumnName())
                .mapTo(Long.class).one() > 0);
    }
}
