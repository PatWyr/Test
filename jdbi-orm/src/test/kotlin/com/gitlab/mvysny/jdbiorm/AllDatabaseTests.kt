package com.gitlab.mvysny.jdbiorm

import com.github.mvysny.dynatest.*
import com.gitlab.mvysny.jdbiorm.condition.conditionTests
import org.jdbi.v3.core.Handle
import java.io.IOException
import kotlin.test.expect

/**
 * Only start/stop docker databases once, to speed up tests dramatically.
 */
class AllDatabaseTests : DynaTest({
    withAllDatabases { dbInfo ->
        group("jdbi() tests") {
            jdbiFunTests()
        }
        group("DB Mapping Tests") {
            dbMappingTests()
        }
        group("DAO") {
            dbDaoTests()
        }
        group("JoinTable") {
            joinTableTestSuite()
        }
        group("Conditions") {
           conditionTests(dbInfo)
        }
        group("DaoOfJoin") {
            daoOfJoinTests()
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
        val person: Person2 = db {
            db {
                db {
                    Person2(name = "foo", age = 25).apply { save() }
                }
            }
        }
        expect(listOf(person)) { Person2.findAll() }
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
