package com.gitlab.mvysny.jdbiorm.condition;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

public final class In implements Condition {
    @NotNull
    private final Expression<?> arg1;
    @NotNull
    private final Collection<? extends Expression<?>> values;

    public In(@NotNull Expression<?> arg1, @NotNull Collection<? extends Expression<?>> values) {
        this.arg1 = Objects.requireNonNull(arg1);
        this.values = Objects.requireNonNull(values);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        In in = (In) o;
        return Objects.equals(arg1, in.arg1) && Objects.equals(values, in.values);
    }

    @Override
    public int hashCode() {
        return Objects.hash(arg1, values);
    }

    @Override
    public String toString() {
        return arg1 + " IN (" + values.stream().map(Objects::toString).collect(Collectors.joining(", ")) + ")";
    }

    @NotNull
    public Expression<?> getArg1() {
        return arg1;
    }

    public @NotNull Collection<?> getValues() {
        return values;
    }
}
