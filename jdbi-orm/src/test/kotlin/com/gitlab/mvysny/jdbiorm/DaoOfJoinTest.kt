package com.gitlab.mvysny.jdbiorm

import com.github.mvysny.dynatest.DynaNodeGroup
import com.github.mvysny.dynatest.DynaTest
import com.github.mvysny.dynatest.DynaTestDsl
import com.github.mvysny.dynatest.expectList
import kotlin.test.expect

@DynaTestDsl
fun DynaNodeGroup.daoOfJoinTests() {
    group("JoinOutcome") {
        joinOutcomeTests()
    }
    group("NestedJoinOutcome") {
        nestedJoinOutcomeTests()
    }
}

@DynaTestDsl
private fun DynaNodeGroup.nestedJoinOutcomeTests() {
    test("smoke") {
        val p = Person2(name = "Foo")
        p.create()
        val d = EntityWithAliasedId("My department")
        d.create()
        MappingTable(p.id!!, d.id!!, "").create()

        val joinOutcomes = NestedJoinOutcome.dao.findAllCustom()
        expect(1) { joinOutcomes.size }
        expect(p) { joinOutcomes[0].person }
        expect(d) { joinOutcomes[0].department }
    }

    test("findAll") {
        val p = Person2(name = "Foo")
        p.create()
        val d = EntityWithAliasedId("My department")
        d.create()
        MappingTable(p.id!!, d.id!!, "").create()

        var joinOutcomes = NestedJoinOutcome.dao.findAll()
        expect(1) { joinOutcomes.size }
        expect(p) { joinOutcomes[0].person }
        expect(d) { joinOutcomes[0].department }

        joinOutcomes = NestedJoinOutcome.dao.findAll(null, 1L, null)
        expectList() { joinOutcomes }

        joinOutcomes = NestedJoinOutcome.dao.findAll(null, 0L, 1L)
        expect(1) { joinOutcomes.size }
        expect(p) { joinOutcomes[0].person }
        expect(d) { joinOutcomes[0].department }

        joinOutcomes = NestedJoinOutcome.dao.findAll(listOf(EntityWithAliasedId.ID.tableAlias("d").asc(), Person2.ID.desc()), null, null)
        expect(1) { joinOutcomes.size }
        expect(p) { joinOutcomes[0].person }
        expect(d) { joinOutcomes[0].department }
    }

    test("findAllBy") {
        val p = Person2(name = "Foo")
        p.create()
        val d = EntityWithAliasedId("My department")
        d.create()
        MappingTable(p.id!!, d.id!!, "").create()

        var joinOutcomes = NestedJoinOutcome.dao.findAllBy(Person2.NAME.eq("Foo"))
        expect(1) { joinOutcomes.size }
        expect(p) { joinOutcomes[0].person }
        expect(d) { joinOutcomes[0].department }
        joinOutcomes = NestedJoinOutcome.dao.findAllBy(NestedJoinOutcome.DEPARTMENT_NAME.eq("Foo"))
        expectList() { joinOutcomes }

        joinOutcomes = NestedJoinOutcome.dao.findAllBy(Person2.NAME.eq("Foo"), 1L, null)
        expectList() { joinOutcomes }

        joinOutcomes = NestedJoinOutcome.dao.findAllBy(Person2.NAME.eq("Foo"), 0L, 1L)
        expect(1) { joinOutcomes.size }
        expect(p) { joinOutcomes[0].person }
        expect(d) { joinOutcomes[0].department }

        joinOutcomes = NestedJoinOutcome.dao.findAllBy(Person2.NAME.eq("Foo"), listOf(EntityWithAliasedId.ID.tableAlias("d").asc(), Person2.ID.desc()), null, null)
        expect(1) { joinOutcomes.size }
        expect(p) { joinOutcomes[0].person }
        expect(d) { joinOutcomes[0].department }
    }

    test("count") {
        expect(0) { NestedJoinOutcome.dao.count() }

        val p = Person2(name = "Foo")
        p.create()
        val d = EntityWithAliasedId("My department")
        d.create()
        MappingTable(p.id!!, d.id!!, "").create()
        expect(1) { NestedJoinOutcome.dao.count() }
    }

    test("countBy") {
        expect(0) { NestedJoinOutcome.dao.countBy(Person2.NAME.eq("Foo")) }

        val p = Person2(name = "Foo")
        p.create()
        val d = EntityWithAliasedId("My department")
        d.create()
        MappingTable(p.id!!, d.id!!, "").create()
        expect(1) { NestedJoinOutcome.dao.countBy(Person2.NAME.eq("Foo")) }
        expect(0) { NestedJoinOutcome.dao.countBy(Person2.NAME.eq("Bar").and(NestedJoinOutcome.DEPARTMENT_NAME.eq("ksada")).and(NestedJoinOutcome.DEPARTMENT_ID.eq(25))) }
    }

    test("existsAny") {
        expect(false) { NestedJoinOutcome.dao.existsAny() }

        val p = Person2(name = "Foo")
        p.create()
        val d = EntityWithAliasedId("My department")
        d.create()
        MappingTable(p.id!!, d.id!!, "").create()
        expect(true) { NestedJoinOutcome.dao.existsAny() }
    }

    test("existsBy") {
        expect(false) { NestedJoinOutcome.dao.existsBy(Person2.NAME.eq("Foo")) }

        val p = Person2(name = "Foo")
        p.create()
        val d = EntityWithAliasedId("My department")
        d.create()
        MappingTable(p.id!!, d.id!!, "").create()
        expect(true) { NestedJoinOutcome.dao.existsBy(Person2.NAME.eq("Foo")) }
        expect(false) { NestedJoinOutcome.dao.existsBy(Person2.NAME.eq("Bar")) }
    }
}

private fun JoinOutcome.expect(p: Person2, d: EntityWithAliasedId) {
    expect(p.name, toString()) { personName }
    expect(p.id, toString()) { personId }
    expect(d.name, toString()) { departmentName }
    expect(d.id, toString()) { departmentId }
}

@DynaTestDsl
private fun DynaNodeGroup.joinOutcomeTests() {
    test("smoke") {
        val p = Person2(name = "Foo")
        p.create()
        val d = EntityWithAliasedId("My department")
        d.create()
        MappingTable(p.id!!, d.id!!, "").create()

        val joinOutcomes = JoinOutcome.dao.findAll()
        expect(1) { joinOutcomes.size }
        joinOutcomes[0].expect(p, d)
    }

    test("findAll") {
        val p = Person2(name = "Foo")
        p.create()
        val d = EntityWithAliasedId("My department")
        d.create()
        MappingTable(p.id!!, d.id!!, "").create()

        var joinOutcomes = JoinOutcome.dao.findAll()
        expect(1) { joinOutcomes.size }
        joinOutcomes[0].expect(p, d)

        joinOutcomes = JoinOutcome.dao.findAll(null, 1L, null)
        expectList() { joinOutcomes }

        joinOutcomes = JoinOutcome.dao.findAll(null, 0L, 1L)
        expect(1) { joinOutcomes.size }
        joinOutcomes[0].expect(p, d)

        joinOutcomes = JoinOutcome.dao.findAll(listOf(JoinOutcome.DEPARTMENT_ID.asc(), Person2.ID.desc()), null, null)
        expect(1) { joinOutcomes.size }
        joinOutcomes[0].expect(p, d)
    }

    test("findAllBy") {
        val p = Person2(name = "Foo")
        p.create()
        val d = EntityWithAliasedId("My department")
        d.create()
        MappingTable(p.id!!, d.id!!, "").create()

        var joinOutcomes = JoinOutcome.dao.findAllBy(Person2.NAME.eq("Foo"))
        expect(1) { joinOutcomes.size }
        joinOutcomes[0].expect(p, d)
        joinOutcomes = JoinOutcome.dao.findAllBy(JoinOutcome.DEPARTMENT_NAME.eq("Foo"))
        expectList() { joinOutcomes }

        joinOutcomes = JoinOutcome.dao.findAllBy(Person2.NAME.eq("Foo"), 1L, null)
        expectList() { joinOutcomes }

        joinOutcomes = JoinOutcome.dao.findAllBy(Person2.NAME.eq("Foo"), 0L, 1L)
        expect(1) { joinOutcomes.size }
        joinOutcomes[0].expect(p, d)

        joinOutcomes = JoinOutcome.dao.findAllBy(Person2.NAME.eq("Foo"), listOf(JoinOutcome.DEPARTMENT_ID.asc(), JoinOutcome.DEPARTMENT_NAME.desc(), Person2.ID.desc()), null, null)
        expect(1) { joinOutcomes.size }
        joinOutcomes[0].expect(p, d)
    }

    test("count") {
        expect(0) { JoinOutcome.dao.count() }

        val p = Person2(name = "Foo")
        p.create()
        val d = EntityWithAliasedId("My department")
        d.create()
        MappingTable(p.id!!, d.id!!, "").create()
        expect(1) { JoinOutcome.dao.count() }
    }

    test("countBy") {
        expect(0) { JoinOutcome.dao.countBy(Person2.NAME.eq("Foo")) }

        val p = Person2(name = "Foo")
        p.create()
        val d = EntityWithAliasedId("My department")
        d.create()
        MappingTable(p.id!!, d.id!!, "").create()
        expect(1) { JoinOutcome.dao.countBy(Person2.NAME.eq("Foo")) }
        expect(0) { JoinOutcome.dao.countBy(Person2.NAME.eq("Bar").and(JoinOutcome.DEPARTMENT_ID.eq(5)).and(JoinOutcome.DEPARTMENT_NAME.eq("adfafs"))) }
    }

    test("existsAny") {
        expect(false) { JoinOutcome.dao.existsAny() }

        val p = Person2(name = "Foo")
        p.create()
        val d = EntityWithAliasedId("My department")
        d.create()
        MappingTable(p.id!!, d.id!!, "").create()
        expect(true) { JoinOutcome.dao.existsAny() }
    }

    test("existsBy") {
        expect(false) { JoinOutcome.dao.existsBy(Person2.NAME.eq("Foo")) }

        val p = Person2(name = "Foo")
        p.create()
        val d = EntityWithAliasedId("My department")
        d.create()
        MappingTable(p.id!!, d.id!!, "").create()
        expect(true) { JoinOutcome.dao.existsBy(Person2.NAME.eq("Foo")) }
        expect(false) { JoinOutcome.dao.existsBy(Person2.NAME.eq("Bar")) }
    }
}
