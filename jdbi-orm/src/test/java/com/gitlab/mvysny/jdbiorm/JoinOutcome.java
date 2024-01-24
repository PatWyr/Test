package com.gitlab.mvysny.jdbiorm;

import org.jdbi.v3.core.mapper.reflect.ColumnName;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

/**
 * Maps a simple join Person.id --1:1-- MappingTable.personId; MappingTable.departmentId --1:1-- EntityWithAliasedId.myid
 * <p></p>
 * Note the clashing column {@link Person2#getName()} and {@link EntityWithAliasedId#getName()}.
 */
public class JoinOutcome implements Serializable {
    public static final @NotNull Property<Long> DEPARTMENT_ID = EntityWithAliasedId.ID.tableAlias("d");
    public static final @NotNull Property<String> DEPARTMENT_NAME = EntityWithAliasedId.NAME.tableAlias("d");

    @ColumnName("id")
    public Long personId;

    @ColumnName("name")
    public String personName;

    @ColumnName("department_myid")
    public Long departmentId;

    @ColumnName("department_name")
    public String departmentName;

    @Override
    public String toString() {
        return "JoinOutcome{" +
                "personId=" + personId +
                ", personName='" + personName + '\'' +
                ", departmentId=" + departmentId +
                ", departmentName='" + departmentName + '\'' +
                '}';
    }

    public static class MyDao extends DaoOfJoin<JoinOutcome> {

        public MyDao() {
            // use both table aliases (EntityWithAliasedId d) and table real names (Test), to test
            // both qualified names and table aliases API.
            super(JoinOutcome.class, "select Test.id, Test.name, d.myid as department_myid, d.name as department_name\n" +
                    "FROM Test join mapping_table m on Test.id = m.person_id join EntityWithAliasedId d on m.department_id = d.myid\n");
        }
    }

    @NotNull
    public static final MyDao dao = new MyDao();
}
