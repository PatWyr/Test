package com.gitlab.mvysny.jdbiorm;

import org.jdbi.v3.core.mapper.Nested;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.List;

import static com.gitlab.mvysny.jdbiorm.JdbiOrm.jdbi;

/**
 * Maps a simple join Person.id --1:1-- MappingTable.personId; MappingTable.departmentId --1:1-- EntityWithAliasedId.myid
 * <p></p>
 * Note the clashing column {@link Person2#getName()} and {@link EntityWithAliasedId#getName()}.
 */
public class NestedJoinOutcome implements Serializable {
    @Nested
    private Person2 person = new Person2();
    @Nested("department_")
    private EntityWithAliasedId department = new EntityWithAliasedId();

    public Person2 getPerson() {
        return person;
    }

    public void setPerson(Person2 person) {
        this.person = person;
    }

    public EntityWithAliasedId getDepartment() {
        return department;
    }

    public void setDepartment(EntityWithAliasedId department) {
        this.department = department;
    }

    @Override
    public String toString() {
        return "NestedJoinOutcome{" +
                "person=" + person +
                ", department=" + department +
                '}';
    }

    public static class MyDao extends DaoOfAny<NestedJoinOutcome> {

        public MyDao() {
            super(NestedJoinOutcome.class);
        }

        @NotNull
        public List<NestedJoinOutcome> findAllCustom() {
            return jdbi().withHandle(handle -> handle
                    .createQuery("select p.*, d.myid as department_myid, d.name as department_name\n" +
                            "FROM Test p join mapping_table m on p.id = m.person_id join EntityWithAliasedId d on m.department_id = d.myid\n" +
                            "ORDER BY d.myid ASC")
                    .map(getRowMapper())
                    .list());
        }
    }

    @NotNull
    public static final MyDao dao = new MyDao();
}
