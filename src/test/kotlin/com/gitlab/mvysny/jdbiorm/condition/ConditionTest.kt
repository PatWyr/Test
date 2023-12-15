package com.gitlab.mvysny.jdbiorm.condition

import com.github.mvysny.dynatest.DynaNodeGroup
import com.github.mvysny.dynatest.DynaTest
import com.github.mvysny.dynatest.DynaTestDsl
import com.gitlab.mvysny.jdbiorm.JoinTable
import com.gitlab.mvysny.jdbiorm.Person
import kotlin.test.expect

class ConditionTest : DynaTest({
    group("toString") {
        test("simple") {
            expect("Person.id") { Person.ID.toString() }
        }
        test("expressions") {
            expect("Person.id = 5") { Person.ID.eq(5L).toString() }
            expect("(NOT(Person.id = 5)) AND (Person.id = 6)") { Person.ID.eq(5L).not().and(Person.ID.eq(6L)).toString() }
        }
    }
})

@DynaTestDsl
fun DynaNodeGroup.conditionTests() {
    // check that the produced SQL actually executes and is accepted by the database. We don't test the
    // correctness of the result just yet
    group("smoke") {
        test("simple conditions") {
            JoinTable.dao.findAllBy(JoinTable.ORDERID.eq(1))
            JoinTable.dao.findAllBy(JoinTable.ORDERID.eq(JoinTable.CUSTOMERID))
            JoinTable.dao.findAllBy(JoinTable.ORDERID.lt(1))
            JoinTable.dao.findAllBy(JoinTable.ORDERID.lt(JoinTable.CUSTOMERID))
            JoinTable.dao.findAllBy(JoinTable.ORDERID.le(1))
            JoinTable.dao.findAllBy(JoinTable.ORDERID.le(JoinTable.CUSTOMERID))
            JoinTable.dao.findAllBy(JoinTable.ORDERID.gt(1))
            JoinTable.dao.findAllBy(JoinTable.ORDERID.gt(JoinTable.CUSTOMERID))
            JoinTable.dao.findAllBy(JoinTable.ORDERID.ge(1))
            JoinTable.dao.findAllBy(JoinTable.ORDERID.ge(JoinTable.CUSTOMERID))
            JoinTable.dao.findAllBy(JoinTable.ORDERID.ne(1))
            JoinTable.dao.findAllBy(JoinTable.ORDERID.ne(JoinTable.CUSTOMERID))
            JoinTable.dao.findAllBy(JoinTable.ORDERID.`in`(1, 2, 3))
            JoinTable.dao.findAllBy(JoinTable.ORDERID.`in`(JoinTable.CUSTOMERID))
            JoinTable.dao.findAllBy(JoinTable.ORDERID.notIn(1, 2, 3))
            JoinTable.dao.findAllBy(JoinTable.ORDERID.notIn(JoinTable.CUSTOMERID))
            JoinTable.dao.findAllBy(JoinTable.ORDERID.between(1, 10))
            JoinTable.dao.findAllBy(JoinTable.ORDERID.notBetween(1, 10))
            Person.dao.findAllBy(Person.ISALIVE25.isTrue())
            Person.dao.findAllBy(Person.ISALIVE25.isFalse())
            Person.dao.findAllBy(Person.NAME.equalIgnoreCase("Foo"))
            Person.dao.findAllBy(Person.NAME.notEqualIgnoreCase("Bar"))
            Person.dao.findAllBy(Person.NAME.like("%Bar"))
            Person.dao.findAllBy(Person.NAME.likeIgnoreCase("%Baz"))
            Person.dao.findAllBy(Person.NAME.isNull())
            Person.dao.findAllBy(Person.NAME.isNotNull())
        }
        test("combining") {
            JoinTable.dao.findAllBy(JoinTable.ORDERID.eq(1).and(JoinTable.CUSTOMERID.eq(2)))
            JoinTable.dao.findAllBy(JoinTable.ORDERID.eq(1).or(JoinTable.CUSTOMERID.eq(2)))
            JoinTable.dao.findAllBy(JoinTable.ORDERID.eq(1).and(JoinTable.CUSTOMERID.eq(2)).not())
        }
    }
}
