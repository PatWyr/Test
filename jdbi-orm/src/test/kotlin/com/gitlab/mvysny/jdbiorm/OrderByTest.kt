package com.gitlab.mvysny.jdbiorm

import com.github.mvysny.dynatest.DynaTest
import kotlin.test.expect

class OrderByTest : DynaTest({
    test("equals") {
        expect(true) { Person.ID.asc() == Person.ID.asc() }
        expect(false) { Person.ID.desc() == Person.ID.asc() }
        expect(false) { Person.NAME.asc() == Person.ID.asc() }
        expect(false) { Person.NAME.asc() == Person.ID.desc() }
    }
})
