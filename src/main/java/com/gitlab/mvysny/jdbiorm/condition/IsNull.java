package com.gitlab.mvysny.jdbiorm.condition;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class IsNull implements Condition {
    @NotNull
    private final Expression<?> arg;

    public IsNull(@NotNull Expression<?> arg) {
        this.arg = Objects.requireNonNull(arg);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IsNull isTrue = (IsNull) o;
        return Objects.equals(arg, isTrue.arg);
    }

    @Override
    public int hashCode() {
        return Objects.hash(arg);
    }

    @Override
    public String toString() {
        return arg + " IS NULL";
    }

    public @NotNull Expression<?> getArg() {
        return arg;
    }
}
