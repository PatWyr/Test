package com.gitlab.mvysny.jdbiorm.condition;

import com.gitlab.mvysny.jdbiorm.Property;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

public final class In implements Condition {
    @NotNull
    private final Property<?> property;
    @NotNull
    private final Collection<Property<?>> values;

    public In(@NotNull Property<?> property, @NotNull Collection<Property<?>> values) {
        this.property = Objects.requireNonNull(property);
        this.values = Objects.requireNonNull(values);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        In in = (In) o;
        return Objects.equals(property, in.property) && Objects.equals(values, in.values);
    }

    @Override
    public int hashCode() {
        return Objects.hash(property, values);
    }

    @Override
    public String toString() {
        return property + " IN (" + values.stream().map(Objects::toString).collect(Collectors.joining(", ")) + ")";
    }

    @NotNull
    public Property<?> getProperty() {
        return property;
    }

    public @NotNull Collection<?> getValues() {
        return values;
    }
}
