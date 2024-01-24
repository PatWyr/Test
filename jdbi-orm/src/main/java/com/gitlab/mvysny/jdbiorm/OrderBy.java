package com.gitlab.mvysny.jdbiorm;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/**
 * An <code>ORDER BY</code> SQL clause. Useful for programmatic SQL construction.
 * See {@link DaoOfAny#findAll(List, Long, Long)} for more details.
 * <p></p>
 * Recommended way to create this object is to call {@link Property#asc()} or {@link Property#desc()};
 * usually on instances of {@link TableProperty}.
 */
public final class OrderBy implements Serializable {
    /**
     * The order of the sorting, ascending or descending.
     */
    public enum Order {
        ASC, DESC
    }
    @NotNull
    public static final Order ASC = Order.ASC;
    @NotNull
    public static final Order DESC = Order.DESC;

    @NotNull
    private final Property<?> property;
    @NotNull
    private final Order order;

    /**
     * Creates the order-by clause
     * @param property the owner property to sort by, not null.
     * @param order the ordering, not null.
     */
    public OrderBy(@NotNull Property<?> property, @NotNull Order order) {
        this.property = Objects.requireNonNull(property);
        this.order = Objects.requireNonNull(order);
    }

    /**
     * Creates a sorting clause which sorts by given entity property.
     * @param entityClass the entity class
     * @param propertyName the property name, see {@link Property.Name} for details on the naming scheme.
     * @param order the desired order
     * @return the order clause, not null.
     */
    @NotNull
    public static OrderBy of(@NotNull Class<?> entityClass, @NotNull String propertyName, @NotNull Order order) {
        return new OrderBy(TableProperty.of(entityClass, propertyName), order);
    }

    /**
     * The property to order by.
     * @return The property to order by.
     */
    @NotNull
    public Property<?> getProperty() {
        return property;
    }

    /**
     * The order of the sorting, ascending or descending.
     * @return The order of the sorting, ascending or descending.
     */
    @NotNull
    public Order getOrder() {
        return order;
    }

    @Override
    public String toString() {
        return property + " " + order;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OrderBy)) return false;
        OrderBy orderBy = (OrderBy) o;
        return Objects.equals(property, orderBy.property) && order == orderBy.order;
    }

    @Override
    public int hashCode() {
        return Objects.hash(property, order);
    }
}
