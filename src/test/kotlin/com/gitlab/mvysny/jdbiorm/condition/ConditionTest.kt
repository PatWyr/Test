package com.gitlab.mvysny.jdbiorm.condition

import com.github.mvysny.dynatest.DynaTest
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
