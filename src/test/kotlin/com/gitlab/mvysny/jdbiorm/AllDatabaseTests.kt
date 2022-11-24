package com.gitlab.mvysny.jdbiorm

import com.github.mvysny.dynatest.*
import org.jdbi.v3.core.Handle
import java.io.IOException
import kotlin.test.expect

/**
 * Only start/stop docker databases once, to speed up tests dramatically.
 */
class AllDatabaseTests : DynaTest({
    withAllDatabases {
        group("jdbi() tests") {
            jdbiFunTests()
        }
        group("DB Mapping Tests") {
            dbMappingTests()
        }
        group("DAO") {
            dbDaoTests()
        }
    }
})

/**
 * Tests the `db{}` method whether it manages transactions properly.
 */
@DynaTestDsl
fun DynaNodeGroup.jdbiFunTests() {
    test("verifyEntityManagerClosed") {
        val em: Handle = db { this }
        expect(true) { em.connection.isClosed }
    }
    test("exceptionRollsBack") {
        expectThrows(IOException::class) {
            db {
                Person(name = "foo", age = 25).save()
                expectList(25) { db { Person.findAll().map { it.age } } }
                throw IOException("simulated")
            }
        }
        expect(listOf()) { db { Person.findAll() } }
    }
    test("commitInNestedDbBlocks") {
        val person: Person = db {
            db {
                db {
                    Person(name = "foo", age = 25).apply { save() }
                }
            }
        }
        expect(listOf(person.withZeroNanos())) {
            db {
                Person.findAll().map { it.withZeroNanos() }
            }
        }
    }
    test("exceptionRollsBackInNestedDbBlocks") {
        expectThrows(IOException::class) {
            db {
                db {
                    db {
                        Person(name = "foo", age = 25).save()
                        throw IOException("simulated")
                    }
                }
            }
        }
        expect(listOf()) { Person.findAll() }
    }
}
