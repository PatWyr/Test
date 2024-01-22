package com.gitlab.mvysny.jdbiorm;

import jakarta.validation.constraints.NotNull;
import org.jdbi.v3.core.annotation.JdbiProperty;
import org.jdbi.v3.core.mapper.reflect.ColumnName;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Tests for https://github.com/mvysny/vok-orm/issues/7
 */
public class EntityWithAliasedId implements Entity<Long> {
    @ColumnName("myid")
    private Long id;

    private String name;

    public EntityWithAliasedId(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public EntityWithAliasedId(String name) {
        this.name = name;
    }

    public EntityWithAliasedId() {
    }

    @Nullable
    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public static final Dao<EntityWithAliasedId, Long> dao = new Dao<>(EntityWithAliasedId.class);

    @Override
    public String toString() {
        return "EntityWithAliasedId{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EntityWithAliasedId that = (EntityWithAliasedId) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }

    @NotNull
    @JdbiProperty(map = false)
    public static final TableProperty<EntityWithAliasedId, String> ID = TableProperty.of(EntityWithAliasedId.class, "id");
}
