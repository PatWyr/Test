package com.gitlab.mvysny.jdbiorm;

import org.jdbi.v3.core.mapper.Nested;
import org.jdbi.v3.core.mapper.reflect.ColumnName;
import org.jetbrains.annotations.Nullable;

import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Objects;

/**
 * Demoes a mapping table with composite primary key.
 *
 * @author mavi
 */
@Table("mapping_table")
public class MappingTable implements Entity<MappingTable.ID> {

    public MappingTable() {
    }

    public MappingTable(long personId, long departmentId, @NotNull String someData) {
        this.id = new ID(personId, departmentId);
        this.someData = someData;
    }

    public static class ID implements Serializable {
        @ColumnName("person_id")
        @NotNull
        public Long personId;
        @ColumnName("department_id")
        @NotNull
        public Long departmentId;

        public ID(@NotNull Long personId, @NotNull Long departmentId) {
            this.personId = personId;
            this.departmentId = departmentId;
        }

        public ID() {
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ID id = (ID) o;
            return Objects.equals(personId, id.personId) &&
                    Objects.equals(departmentId, id.departmentId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(personId, departmentId);
        }

        @Override
        public String toString() {
            return personId + ", " + departmentId;
        }
    }

    @NotNull
    @Nested
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

    public String getSomeData() {
        return someData;
    }

    public void setSomeData(String someData) {
        this.someData = someData;
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
        return "MappingTable{" + "id=" + id + ", " + someData + '}';
    }

    public static final Dao<MappingTable, ID> dao = new Dao<>(MappingTable.class);
}
