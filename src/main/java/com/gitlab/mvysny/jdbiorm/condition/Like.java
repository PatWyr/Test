package com.gitlab.mvysny.jdbiorm.condition;

import com.gitlab.mvysny.jdbiorm.Property;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class Like implements Condition {
    @NotNull
    private final Expression<?> arg1;
    @NotNull
    private final Expression<?> arg2;

    public Like(@NotNull Expression<?> arg1, @NotNull Expression<?> arg2) {
        this.arg1 = arg1;
        this.arg2 = arg2;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Like like = (Like) o;
        return Objects.equals(arg1, like.arg1) && Objects.equals(arg2, like.arg2);
    }

    @Override
    public int hashCode() {
        return Objects.hash(arg1, arg2);
    }

    @Override
    public String toString() {
        return arg1 + " LIKE " + arg2;
    }

    public @NotNull Expression<?> getArg1() {
        return arg1;
    }

    public @NotNull Expression<?> getArg2() {
        return arg2;
    }
}
