package com.gitlab.mvysny.jdbiorm;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Objects;

import static com.gitlab.mvysny.jdbiorm.JdbiOrm.jdbi;

/**
 * Join table which has no PK on its own.
 *
 * @author mavi
 */
@Table("JOIN_TABLE")
public class JoinTable implements Serializable {
    @NotNull
    public static final TableProperty<JoinTable, Integer> CUSTOMERID = TableProperty.of(JoinTable.class, "customerId");
    @NotNull
    public static final TableProperty<JoinTable, Integer> ORDERID = TableProperty.of(JoinTable.class, "orderId");
    private Integer customerId;
    private Integer orderId;

    public JoinTable() {
    }

    public JoinTable(Integer customerId, Integer orderId) {
        this.customerId = customerId;
        this.orderId = orderId;
    }

    public Integer getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Integer customerId) {
        this.customerId = customerId;
    }

    public Integer getOrderId() {
        return orderId;
    }

    public void setOrderId(Integer orderId) {
        this.orderId = orderId;
    }

    @Override
    public String toString() {
        return "JoinTable{" +
                "customerId=" + customerId +
                ", orderId=" + orderId +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JoinTable joinTable = (JoinTable) o;
        return Objects.equals(customerId, joinTable.customerId) &&
                Objects.equals(orderId, joinTable.orderId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(customerId, orderId);
    }

    public static final DaoOfAny<JoinTable> dao = new DaoOfAny<>(JoinTable.class);

    public void save() {
        jdbi().withHandle(handle -> handle
                .createUpdate("insert into JOIN_TABLE (customerId, orderId) VALUES (:customerId, :orderId)")
                .bindBean(this)
                .execute());
    }
}
