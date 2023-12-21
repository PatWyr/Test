package com.gitlab.mvysny.jdbiorm

import com.github.mvysny.dynatest.DynaTest
import kotlin.test.expect

private val String.asc: OrderBy get() = OrderBy(Property.Name(this), OrderBy.ASC)
private val String.desc: OrderBy get() = OrderBy(Property.Name(this), OrderBy.DESC)

class OrderByTest : DynaTest({
    test("equals") {
        expect(true) { "foo".asc == "foo".asc }
        expect(false) { "foo".asc == "foo".desc }
        expect(false) { "foo".asc == "bar".asc }
        expect(false) { "foo".asc == "bar".desc }
    }
})
