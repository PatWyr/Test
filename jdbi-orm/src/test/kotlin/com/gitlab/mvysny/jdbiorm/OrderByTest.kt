package com.gitlab.mvysny.jdbiorm

import org.junit.jupiter.api.Test
import kotlin.test.expect

class OrderByTest {
    @Test fun testEquals() {
        expect(true) { Person.ID.asc() == Person.ID.asc() }
        expect(false) { Person.ID.desc() == Person.ID.asc() }
        expect(false) { Person.NAME.asc() == Person.ID.asc() }
        expect(false) { Person.NAME.asc() == Person.ID.desc() }
    }
}
