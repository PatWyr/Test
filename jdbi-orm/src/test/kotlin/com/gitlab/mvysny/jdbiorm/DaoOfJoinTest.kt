package com.gitlab.mvysny.jdbiorm

import com.github.mvysny.dynatest.DynaNodeGroup
import com.github.mvysny.dynatest.DynaTest
import com.github.mvysny.dynatest.DynaTestDsl
import com.github.mvysny.dynatest.expectList
import kotlin.test.expect

@DynaTestDsl
fun DynaNodeGroup.daoOfJoinTests() {
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
        expect(0) { NestedJoinOutcome.dao.countBy(Person2.NAME.eq("Bar")) }
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
