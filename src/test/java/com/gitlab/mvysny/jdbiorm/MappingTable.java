package com.gitlab.mvysny.jdbiorm;

import org.jdbi.v3.core.mapper.reflect.ColumnName;
import org.jetbrains.annotations.Nullable;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Objects;

/**
 * Demoes a mapping table with composite primary key.
 * @author mavi
 */
@Table("mapping_table")
public class MappingTable implements Entity<MappingTable.ID> {
    public static class ID implements Serializable {
        @ColumnName("person_id")
        @NotNull
        public Long personId;
        @ColumnName("department_id")
        @NotNull
        public Long departmentId;
    }

    @NotNull
    private ID id;

    @ColumnName("some_data")
    @NotNull
    private String someData;

    @Nullable
    @Override
    public ID getId() {
        return id;
    }

    @Override
    public void setId(ID id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MappingTable that = (MappingTable) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "MappingTable{" +
                "id=" + id + ", " +
                "someData=" + someData +
                '}';
    }
}
