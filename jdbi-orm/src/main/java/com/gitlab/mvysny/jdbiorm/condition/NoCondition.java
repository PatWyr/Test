package com.gitlab.mvysny.jdbiorm.condition;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ObjectStreamException;

/**
 * A direct replacement for `null` condition. When passed to Dao, this effectively
 * removes the WHERE statement as if it was replaced by WHERE 1=1. In theory you could
 * think of this as a TRUE condition, however there is a huge difference: NoCondition OR x == x
 * while TRUE or x == TRUE.
 */
public final class NoCondition implements Condition {
    @NotNull
    public static final NoCondition INSTANCE = new NoCondition();

    private NoCondition() {
    }

    @Override
    public @NotNull Condition not() {
        return this;
    }

    @Override
    public @NotNull Condition or(@Nullable Condition other) {
        return other == null ? this : other;
    }

    @Override
    public @NotNull Condition and(@Nullable Condition other) {
        return other == null ? this : other;
    }

    @Override
    public String toString() {
        return "NoCondition";
    }

    @Override
    public @NotNull ParametrizedSql toSql() {
        throw new UnsupportedOperationException("Can not be converted to a SQL statement");
    }

    @Override
    public boolean test() {
        return true;
    }

    private Object readResolve() throws ObjectStreamException {
        // preserve singleton-ness
        return INSTANCE;
    }
}
