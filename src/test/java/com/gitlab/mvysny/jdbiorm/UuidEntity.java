package com.gitlab.mvysny.jdbiorm;

import java.util.UUID;

/**
 * @author mavi
 */
public interface UuidEntity extends Entity<UUID> {
    @Override
    default void create(boolean validate) {
        setId(UUID.randomUUID());
        Entity.super.create(validate);
    }
}
