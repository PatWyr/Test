package com.gitlab.mvysny.jdbiorm.condition;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * The <code>XOR</code> statement.
 */
final class Xor implements Condition {
    @NotNull
    private final Condition condition1;
    @NotNull
    private final Condition condition2;

    public Xor(@NotNull Condition condition1, @NotNull Condition condition2) {
        this.condition1 = Objects.requireNonNull(condition1);
        this.condition2 = Objects.requireNonNull(condition2);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Xor and = (Xor) o;
        return Objects.equals(condition1, and.condition1) && Objects.equals(condition2, and.condition2);
    }

    @Override
    public int hashCode() {
        return Objects.hash(condition1, condition2);
    }

    @Override
    public String toString() {
        return "(" + condition1 + ") XOR (" + condition2 + ")";
    }

    @NotNull
    public Condition getCondition1() {
        return condition1;
    }

    @NotNull
    public Condition getCondition2() {
        return condition2;
    }
}
