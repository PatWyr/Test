package com.gitlab.mvysny.jdbiorm

import com.github.mvysny.dynatest.DynaTest
import kotlin.test.expect

class OrderByTest : DynaTest({
    test("equals") {
        expect(true) { OrderBy("foo", OrderBy.ASC) == OrderBy("foo", OrderBy.ASC) }
        expect(false) { OrderBy("foo", OrderBy.ASC) == OrderBy("foo", OrderBy.DESC) }
        expect(false) { OrderBy("foo", OrderBy.ASC) == OrderBy("bar", OrderBy.ASC) }
        expect(false) { OrderBy("foo", OrderBy.ASC) == OrderBy("bar", OrderBy.DESC) }
    }
})
