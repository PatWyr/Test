package com.gitlab.mvysny.jdbiorm;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/**
 * An <code>ORDER BY</code> SQL clause. Useful for programmatic SQL construction.
 * See {@link DaoOfAny#findAll(List, Long, Long)} for more details.
 */
public final class OrderBy implements Serializable {
    public enum Order {
        ASC, DESC
    }
    @NotNull
    public static final Order ASC = Order.ASC;
    @NotNull
    public static final Order DESC = Order.DESC;

    /**
     * {@link PropertyMeta#getName()}.
     */
    @NotNull
    private final String name;
    @NotNull
    private final Order order;

    /**
     * Creates the order-by clause
     * @param name {@link PropertyMeta#getName()}. May only contain characters that are valid part of a Java {@link java.lang.reflect.Field#getName}.
     * @param order the ordering, not null.
     */
    public OrderBy(@NotNull String name, @NotNull Order order) {
        this.name = Objects.requireNonNull(name);
        // verify that name doesn't contain funny characters
        if (name.indexOf(' ') >= 0 || name.indexOf('\'') >= 0 || name.indexOf('"') >= 0) {
            throw new IllegalArgumentException("Parameter name: invalid value " + name + ": must not contain weird characters");
        }
        this.order = Objects.requireNonNull(order);
    }

    /**
     * {@link PropertyMeta#getName()}.
     * @return {@link PropertyMeta#getName()}.
     */
    @NotNull
    public String getName() {
        return name;
    }

    @NotNull
    public Order getOrder() {
        return order;
    }

    @Override
    public String toString() {
        return name + " " + order;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderBy orderBy = (OrderBy) o;
        return name.equals(orderBy.name) && order == orderBy.order;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, order);
    }
}
