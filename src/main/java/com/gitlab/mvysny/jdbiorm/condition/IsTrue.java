package com.gitlab.mvysny.jdbiorm.condition;

import com.gitlab.mvysny.jdbiorm.Property;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Create a condition to check this field against known string literals for
 * <code>true</code>.
 * <p>
 * SQL:
 * <code>lower(this) in ("1", "y", "yes", "true", "on", "enabled")</code>
 */
public final class IsTrue implements Condition {
    @NotNull
    private final Property<?> arg;

    public IsTrue(@NotNull Property<?> arg) {
        this.arg = arg;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IsTrue isTrue = (IsTrue) o;
        return Objects.equals(arg, isTrue.arg);
    }

    @Override
    public int hashCode() {
        return Objects.hash(arg);
    }

    @Override
    public String toString() {
        return arg + " IS TRUE";
    }
}
