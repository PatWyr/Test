package com.gitlab.mvysny.jdbiorm;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.mapper.reflect.BeanMapper;
import org.jdbi.v3.core.mapper.reflect.FieldMapper;
import org.jdbi.v3.core.statement.Binding;
import org.jdbi.v3.core.statement.Query;
import org.jdbi.v3.core.statement.SqlStatement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

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
        return FieldMapper.of(entityClass);
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
        final T result = findById(id);
        if (result == null) {
            throw new IllegalArgumentException("There is no " + entityClass.getSimpleName() + " for id " + id);
        }
        return result;
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
     * @todo
     */
    @Nullable
    public T findOneBy(@NotNull String where, Consumer<Query> queryConsumer) {
        return jdbi().withHandle(handle -> {
                    final Query query = handle.createQuery("select * from <TABLE> where <WHERE>")
                            .define("TABLE", meta.getDatabaseTableName())
                            .define("WHERE", where);
                    queryConsumer.accept(query);
                    return query
                            .map(getRowMapper())
                            .findOne().orElse(null);
                }
        );
    }

    /**
     * @todo
     */
    @NotNull
    public T getOneBy(@NotNull String where, Consumer<Query> queryConsumer) {
        return jdbi().withHandle(handle -> {
                    final Query query = handle.createQuery("select * from <TABLE> where <WHERE>")
                            .define("TABLE", meta.getDatabaseTableName())
                            .define("WHERE", where);
                    queryConsumer.accept(query);
                    return query
                            .map(getRowMapper())
                            .findOne().orElseThrow(() -> new IllegalArgumentException("no " + entityClass.getSimpleName()
                                    + " satisfying '" + where + "'" + toString(query)));
                }
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
    public long count() {
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
    /**
     * Deletes row with given ID. Does nothing if there is no such row.
     */
    public void deleteById(@NotNull ID id) {
        jdbi().withHandle(handle -> handle.createUpdate("delete from <TABLE> where <ID>=:id")
                .define("TABLE", meta.getDatabaseTableName())
                .define("ID", meta.getIdProperty().getDbColumnName())
                .bind("id", id)
                .execute());
    }

    @NotNull
    protected String toString(@NotNull SqlStatement<?> statement) {
        try {
            final Method getBinding = SqlStatement.class.getDeclaredMethod("getBinding");
            getBinding.setAccessible(true);
            final Binding binding = (Binding) getBinding.invoke(statement);
            return binding.toString();
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
}
