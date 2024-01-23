package com.gitlab.mvysny.jdbiorm

import com.github.mvysny.dynatest.DynaTest
import com.github.mvysny.dynatest.cloneBySerialization
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
})
