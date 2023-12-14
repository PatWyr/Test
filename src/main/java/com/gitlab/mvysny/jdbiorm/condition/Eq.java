package com.gitlab.mvysny.jdbiorm.condition;

import com.gitlab.mvysny.jdbiorm.Property;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class Eq implements Condition {
    @NotNull
    private final Property<?> arg1;
    @NotNull
    private final Property<?> arg2;

    public Eq(@NotNull Property<?> arg1, @NotNull Property<?> arg2) {
        this.arg1 = arg1;
        this.arg2 = arg2;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Eq eq = (Eq) o;
        return Objects.equals(arg1, eq.arg1) && Objects.equals(arg2, eq.arg2);
    }

    @Override
    public int hashCode() {
        return Objects.hash(arg1, arg2);
    }

    @Override
    public String toString() {
        return arg1 + " = " + arg2;
    }
}
