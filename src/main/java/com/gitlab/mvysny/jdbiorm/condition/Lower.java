package com.gitlab.mvysny.jdbiorm.condition;

import com.gitlab.mvysny.jdbiorm.Property;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.Objects;

public final class Lower<V> implements Property<V> {
    @NotNull
    private final Property<V> arg;

    public Lower(@NotNull Property<V> arg) {
        this.arg = arg;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Lower lower = (Lower) o;
        return Objects.equals(arg, lower.arg);
    }

    @Override
    public int hashCode() {
        return Objects.hash(arg);
    }

    @Override
    public String toString() {
        return "LOWER(" + arg + ")";
    }

    public @NotNull Property<V> getArg() {
        return arg;
    }

    @Override
    public @NotNull Name getName() {
        throw new UnsupportedOperationException();
    }

    @Override
    public @NotNull DbName getDbName() {
        throw new UnsupportedOperationException();
    }
}
