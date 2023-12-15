package com.gitlab.mvysny.jdbiorm.condition;

import com.gitlab.mvysny.jdbiorm.Property;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Create a condition to check this field against known string literals for
 * <code>false</code>.
 * <p>
 * SQL:
 * <code>lower(this) in ("0", "n", "no", "false", "off", "disabled")</code>
 */
public final class IsFalse implements Condition {
    @NotNull
    private final Expression<?> arg;

    public IsFalse(@NotNull Expression<?> arg) {
        this.arg = Objects.requireNonNull(arg);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IsFalse isTrue = (IsFalse) o;
        return Objects.equals(arg, isTrue.arg);
    }

    @Override
    public int hashCode() {
        return Objects.hash(arg);
    }

    @Override
    public String toString() {
        return arg + " IS FALSE";
    }

    public @NotNull Expression<?> getArg() {
        return arg;
    }
}
