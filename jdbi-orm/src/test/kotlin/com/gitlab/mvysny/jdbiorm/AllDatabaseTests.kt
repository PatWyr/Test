package com.gitlab.mvysny.jdbiorm

import com.gitlab.mvysny.jdbiorm.condition.AbstractFindByConditionTests
import org.jdbi.v3.core.Handle
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.IOException
import kotlin.test.expect

abstract class AbstractDatabaseTests(val info: DatabaseInfo) {
    @Nested inner class JdbiTests : AbstractJdbiTests()
    @Nested inner class MappingTests : AbstractMappingTests()
    @Nested inner class DbDaoTests : AbstractDbDaoTests()
    @Nested inner class JoinTableTests : AbstractJoinTableTests()
    @Nested inner class FindByConditionTests : AbstractFindByConditionTests(info)
    @Nested inner class DaoOfJoinTests : AbstractDaoOfJoinTests()
}

/**
 * Tests the `db{}` method whether it manages transactions properly.
 */
abstract class AbstractJdbiTests {
    @Test
    fun verifyEntityManagerClosed() {
        val em: Handle = db { this }
        expect(true) { em.connection.isClosed }
    }
    @Test
    fun exceptionRollsBack() {
        assertThrows<IOException> {
            db {
                Person(name = "foo", age = 25).save()
                expectList(25) { db { Person.findAll().map { it.age } } }
                throw IOException("simulated")
            }
        }
        expect(listOf()) { db { Person.findAll() } }
    }
    @Test
    fun commitInNestedDbBlocks() {
        val person: Person2 = db {
            db {
                db {
                    Person2(name = "foo", age = 25).apply { save() }
                }
            }
        }
        expect(listOf(person)) { Person2.findAll() }
    }
    @Test
    fun exceptionRollsBackInNestedDbBlocks() {
        assertThrows<IOException> {
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
