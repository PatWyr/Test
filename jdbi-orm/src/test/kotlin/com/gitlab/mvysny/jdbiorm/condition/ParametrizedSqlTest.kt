package com.gitlab.mvysny.jdbiorm.condition

import org.junit.jupiter.api.Test
import kotlin.test.expect

class ParametrizedSqlTest {
    @Test fun testToString() {
        expect("'true'{}") { ParametrizedSql("true").toString() }
        expect("'a=:aaa'{aaa=5}") { ParametrizedSql("a=:aaa", mapOf("aaa" to 5)).toString() }
        expect("'name = :name AND age = :aaa'{aaa=25, name=foo}") { ParametrizedSql("name = :name AND age = :aaa", mapOf("name" to "foo", "aaa" to 25)).toString() }
    }
    @Test fun testEquals() {
        expect(ParametrizedSql("true")) { ParametrizedSql("true") }
        expect(ParametrizedSql("true", mapOf("a" to 5, "b" to "c"))) { ParametrizedSql("true", mapOf("a" to 5, "b" to "c")) }
        expect(false) { ParametrizedSql("true") == ParametrizedSql("false") }
        expect(false) { ParametrizedSql("true") == ParametrizedSql("true", mapOf("a" to 5)) }
    }
}
