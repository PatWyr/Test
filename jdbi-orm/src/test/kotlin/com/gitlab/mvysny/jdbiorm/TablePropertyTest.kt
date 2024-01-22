package com.gitlab.mvysny.jdbiorm

import com.github.mvysny.dynatest.DynaTest
import com.github.mvysny.dynatest.cloneBySerialization
import kotlin.test.expect

class TablePropertyTest : DynaTest({
    test("serialization") {
        expect(Person.ID) { Person.ID.cloneBySerialization() }
    }
})
