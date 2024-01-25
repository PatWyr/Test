package com.gitlab.mvysny.jdbiorm

import com.github.mvysny.dynatest.DynaTest
import com.github.mvysny.dynatest.cloneBySerialization
import java.time.Instant
import java.util.Date
import kotlin.test.expect

class TablePropertyTest : DynaTest({
    test("serialization") {
        expect(Person.ID) { Person.ID.cloneBySerialization() }
        expect(EntityWithAliasedId.ID) { EntityWithAliasedId.ID.cloneBySerialization() }
        expect(MappingTable.ID_PERSONID) { MappingTable.ID_PERSONID.cloneBySerialization() }
        expect(JoinTable.ORDERID) { JoinTable.ORDERID.cloneBySerialization() }
    }
    test("name") {
        expect("id") { Person.ID.name.name }
        expect("id") { EntityWithAliasedId.ID.name.name }
        expect("id.personId") { MappingTable.ID_PERSONID.name.name }
        expect("orderId") { JoinTable.ORDERID.name.name }
    }
    test("dbName.qualifiedName") {
        expect("Test.id") { Person.ID.dbName.qualifiedName }
        expect("EntityWithAliasedId.myid") { EntityWithAliasedId.ID.dbName.qualifiedName }
        expect("mapping_table.person_id") { MappingTable.ID_PERSONID.dbName.qualifiedName }
        expect("JOIN_TABLE.orderId") { JoinTable.ORDERID.dbName.qualifiedName }
    }
    test("dbName.unqualifiedName") {
        expect("id") { Person.ID.dbName.unqualifiedName }
        expect("myid") { EntityWithAliasedId.ID.dbName.unqualifiedName }
        expect("person_id") { MappingTable.ID_PERSONID.dbName.unqualifiedName }
        expect("orderId") { JoinTable.ORDERID.dbName.unqualifiedName }
    }
    test("calculate") {
        expect(5L) { Person.ID.calculate(Person(id = 5)) }
        expect("foo") { Person.NAME.calculate(Person(name = "foo")) }
        expect(4L) { MappingTable.ID_PERSONID.calculate(MappingTable(4L, 10L, "foo")) }
    }
    test("aliased") {
        expect("d.id") { Person2.ID.tableAlias("d").dbName.qualifiedName }
        expect("id") { Person2.ID.tableAlias("d").dbName.unqualifiedName }
    }
    test("external form") {
        expect(Person2.ID) { Property.fromExternalString(Person2.ID.toExternalString()) }
        expect(Person2.ID.tableAlias("d")) { Property.fromExternalString(Person2.ID.tableAlias("d").toExternalString()) }
    }
    test("value type") {
        expect(String::class.java) { Person2.NAME.valueType }
        expect(java.lang.Long::class.java) { Person2.ID.valueType }
        expect(Int::class.java) { Person2.AGE.valueType }
        expect(java.lang.Boolean::class.java) { Person2.ISALIVE25.valueType }
        expect(MaritalStatus::class.java) { Person.MARITALSTATUS.valueType }
        expect(Date::class.java) { Person.CREATED.valueType }
        expect(Instant::class.java) { Person.MODIFIED.valueType }
    }
})
