package com.gitlab.mvysny.jdbiorm.condition;

import com.gitlab.mvysny.jdbiorm.Property;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Create a condition to check this field against known string literals for
 * <code>false</code>.
 * <p>
 * SQL:
 * <code>lcase(this) in ("0", "n", "no", "false", "off", "disabled")</code>
 */
public final class IsFalse implements Condition {
    @NotNull
    private final Property<?> arg;

    public IsFalse(@NotNull Property<?> arg) {
        this.arg = arg;
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
}
