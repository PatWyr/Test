package com.gitlab.mvysny.jdbiorm

import com.github.mvysny.dynatest.DynaNodeGroup
import com.github.mvysny.dynatest.DynaTest
import com.github.mvysny.dynatest.DynaTestDsl
import kotlin.test.expect

class DaoOfJoinTest : DynaTest({
    usingH2Database()
    daoOfJoinTests()
})

@DynaTestDsl
private fun DynaNodeGroup.daoOfJoinTests() {
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
}
