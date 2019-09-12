package com.gitlab.mvysny.jdbiorm;

import org.jdbi.v3.core.mapper.reflect.ColumnName;
import org.jetbrains.annotations.Nullable;

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
}
