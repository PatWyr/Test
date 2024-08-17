package com.gitlab.mvysny.jdbiorm.condition

import com.gitlab.mvysny.jdbiorm.*
import com.gitlab.mvysny.jdbiorm.quirks.DatabaseVariant
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.expect

abstract class AbstractFindByConditionTests(val dbInfo: DatabaseInfo) {
    // check that the produced SQL actually executes and is accepted by the database. We don't test the
    // correctness of the result just yet
    @Nested inner class Smoke {
        @Test fun simpleConditions() {
            JoinTable.dao.findAllBy(JoinTable.ORDERID.eq(1))
            JoinTable.dao.findAllBy(JoinTable.ORDERID.eq(JoinTable.CUSTOMERID))
            JoinTable.dao.findAllBy(JoinTable.ORDERID.lt(1))
            JoinTable.dao.findAllBy(JoinTable.ORDERID.lt(JoinTable.CUSTOMERID))
            JoinTable.dao.findAllBy(JoinTable.ORDERID.le(1))
            JoinTable.dao.findAllBy(JoinTable.ORDERID.le(JoinTable.CUSTOMERID))
            JoinTable.dao.findAllBy(JoinTable.ORDERID.gt(1))
            JoinTable.dao.findAllBy(JoinTable.ORDERID.gt(JoinTable.CUSTOMERID))
            JoinTable.dao.findAllBy(JoinTable.ORDERID.ge(1))
            JoinTable.dao.findAllBy(JoinTable.ORDERID.ge(JoinTable.CUSTOMERID))
            JoinTable.dao.findAllBy(JoinTable.ORDERID.ne(1))
            JoinTable.dao.findAllBy(JoinTable.ORDERID.ne(JoinTable.CUSTOMERID))
            JoinTable.dao.findAllBy(JoinTable.ORDERID.`in`(1, 2, 3))
            JoinTable.dao.findAllBy(JoinTable.ORDERID.`in`(JoinTable.CUSTOMERID))
            JoinTable.dao.findAllBy(JoinTable.ORDERID.notIn(1, 2, 3))
            JoinTable.dao.findAllBy(JoinTable.ORDERID.notIn(JoinTable.CUSTOMERID))
            JoinTable.dao.findAllBy(JoinTable.ORDERID.between(1, 10))
            JoinTable.dao.findAllBy(JoinTable.ORDERID.notBetween(1, 10))
            Person.dao.findAllBy(Person.ISALIVE25.isTrue())
            Person.dao.findAllBy(Person.ISALIVE25.isFalse())
            Person.dao.findAllBy(Person.NAME.equalIgnoreCase("Foo"))
            Person.dao.findAllBy(Person.NAME.notEqualIgnoreCase("Bar"))
            Person.dao.findAllBy(Person.NAME.like("%Bar"))
            Person.dao.findAllBy(Person.NAME.likeIgnoreCase("%Baz"))
            Person.dao.findAllBy(Person.NAME.isNull())
            Person.dao.findAllBy(Person.NAME.isNotNull())
        }
        @Test fun combining() {
            JoinTable.dao.findAllBy(JoinTable.ORDERID.eq(1).and(JoinTable.CUSTOMERID.eq(2)))
            JoinTable.dao.findAllBy(JoinTable.ORDERID.eq(1).or(JoinTable.CUSTOMERID.eq(2)))
            JoinTable.dao.findAllBy(JoinTable.ORDERID.eq(1).and(JoinTable.CUSTOMERID.eq(2)).not())
        }
    }
    @Test fun native() {
        val person = Person2(name = "Foo", age = 25, isAlive25 = true)
        person.save()
        expectList() { Person2.dao.findAllBy(NativeSQL("name = :name", mapOf("name" to "Bar"))) }
        expectList(person) { Person2.dao.findAllBy(NativeSQL("name = :name", mapOf("name" to "Foo"))) }
        expectList() { Person2.dao.findAllBy(NativeSQL("name = :name AND age = :a", mapOf("name" to "Foo", "a" to 26))) }
        expectList(person) { Person2.dao.findAllBy(NativeSQL("name = :name AND age = :a", mapOf("name" to "Foo", "a" to 25))) }
    }
    @Test fun eq() {
        val person = Person2(name = "Foo", age = 25, isAlive25 = true)
        person.save()
        expectList() { Person2.dao.findAllBy(Person2.NAME.eq("Bar")) }
        expectList(person) { Person2.dao.findAllBy(Person2.NAME.eq("Foo")) }
        expectList() { Person2.dao.findAllBy(Person2.AGE.eq(2)) }
        expectList(person) { Person2.dao.findAllBy(Person2.AGE.eq(25)) }
        expectList() { Person2.dao.findAllBy(Person2.ISALIVE25.eq(false)) }
        expectList(person) { Person2.dao.findAllBy(Person2.ISALIVE25.eq(true)) }
    }
    @Test fun lt() {
        val person = Person2(name = "Foo", age = 25)
        person.save()
        expectList() { Person2.dao.findAllBy(Person2.NAME.lt("Bar")) }
        expectList() { Person2.dao.findAllBy(Person2.NAME.lt("Foo")) }
        expectList(person) { Person2.dao.findAllBy(Person2.NAME.lt("ZZZ")) }
        expectList() { Person2.dao.findAllBy(Person2.AGE.lt(2)) }
        expectList() { Person2.dao.findAllBy(Person2.AGE.lt(25)) }
        expectList(person) { Person2.dao.findAllBy(Person2.AGE.lt(100)) }
    }
    @Test fun le() {
        val person = Person2(name = "Foo", age = 25)
        person.save()
        expectList() { Person2.dao.findAllBy(Person2.NAME.le("Bar")) }
        expectList(person) { Person2.dao.findAllBy(Person2.NAME.le("Foo")) }
        expectList(person) { Person2.dao.findAllBy(Person2.NAME.le("ZZZ")) }
        expectList() { Person2.dao.findAllBy(Person2.AGE.le(2)) }
        expectList(person) { Person2.dao.findAllBy(Person2.AGE.le(25)) }
        expectList(person) { Person2.dao.findAllBy(Person2.AGE.le(100)) }
    }
    @Test fun gt() {
        val person = Person2(name = "Foo", age = 25)
        person.save()
        expectList(person) { Person2.dao.findAllBy(Person2.NAME.gt("Bar")) }
        expectList() { Person2.dao.findAllBy(Person2.NAME.gt("Foo")) }
        expectList() { Person2.dao.findAllBy(Person2.NAME.gt("ZZZ")) }
        expectList(person) { Person2.dao.findAllBy(Person2.AGE.gt(2)) }
        expectList() { Person2.dao.findAllBy(Person2.AGE.gt(25)) }
        expectList() { Person2.dao.findAllBy(Person2.AGE.gt(100)) }
    }
    @Test fun ge() {
        val person = Person2(name = "Foo", age = 25)
        person.save()
        expectList(person) { Person2.dao.findAllBy(Person2.NAME.ge("Bar")) }
        expectList(person) { Person2.dao.findAllBy(Person2.NAME.ge("Foo")) }
        expectList() { Person2.dao.findAllBy(Person2.NAME.ge("ZZZ")) }
        expectList(person) { Person2.dao.findAllBy(Person2.AGE.ge(2)) }
        expectList(person) { Person2.dao.findAllBy(Person2.AGE.ge(25)) }
        expectList() { Person2.dao.findAllBy(Person2.AGE.ge(100)) }
    }
    @Test fun ne() {
        val person = Person2(name = "Foo", age = 25, isAlive25 = false)
        person.save()
        expectList(person) { Person2.dao.findAllBy(Person2.NAME.ne("Bar")) }
        expectList() { Person2.dao.findAllBy(Person2.NAME.ne("Foo")) }
        expectList(person) { Person2.dao.findAllBy(Person2.NAME.ne("ZZZ")) }
        expectList(person) { Person2.dao.findAllBy(Person2.AGE.ne(2)) }
        expectList() { Person2.dao.findAllBy(Person2.AGE.ne(25)) }
        expectList(person) { Person2.dao.findAllBy(Person2.AGE.ne(100)) }
        expectList() { Person2.dao.findAllBy(Person2.ISALIVE25.ne(false)) }
        expectList(person) { Person2.dao.findAllBy(Person2.ISALIVE25.ne(true)) }
    }
    @Test fun `in`() {
        val person = Person2(name = "Foo", age = 25, isAlive25 = false)
        person.save()
        expectList() { Person2.dao.findAllBy(Person2.NAME.`in`("Bar")) }
        expectList(person) { Person2.dao.findAllBy(Person2.NAME.`in`("Foo", "Bar")) }
        expectList() { Person2.dao.findAllBy(Person2.AGE.`in`(2)) }
        expectList(person) { Person2.dao.findAllBy(Person2.AGE.`in`(2, 25)) }
        expectList(person) { Person2.dao.findAllBy(Person2.AGE.`in`(2, 25, 100)) }
        expectList() { Person2.dao.findAllBy(Person2.ISALIVE25.`in`(true)) }
        expectList(person) { Person2.dao.findAllBy(Person2.ISALIVE25.`in`(true, false)) }
    }
    @Test fun notIn() {
        val person = Person2(name = "Foo", age = 25, isAlive25 = false)
        person.save()
        expectList(person) { Person2.dao.findAllBy(Person2.NAME.notIn("Bar")) }
        expectList() { Person2.dao.findAllBy(Person2.NAME.notIn("Foo", "Bar")) }
        expectList(person) { Person2.dao.findAllBy(Person2.AGE.notIn(2)) }
        expectList() { Person2.dao.findAllBy(Person2.AGE.notIn(2, 25)) }
        expectList() { Person2.dao.findAllBy(Person2.AGE.notIn(2, 25, 100)) }
        expectList(person) { Person2.dao.findAllBy(Person2.ISALIVE25.notIn(true)) }
        expectList() { Person2.dao.findAllBy(Person2.ISALIVE25.notIn(true, false)) }
    }
    @Test fun between() {
        val person = Person2(name = "Foo", age = 25, isAlive25 = false)
        person.save()
        expectList() { Person2.dao.findAllBy(Person2.AGE.between(2, 3)) }
        expectList(person) { Person2.dao.findAllBy(Person2.AGE.between(2, 25)) }
        expectList(person) { Person2.dao.findAllBy(Person2.AGE.between(2, 100)) }
    }
    @Test fun notBetween() {
        val person = Person2(name = "Foo", age = 25, isAlive25 = false)
        person.save()
        expectList(person) { Person2.dao.findAllBy(Person2.AGE.notBetween(2, 3)) }
        expectList() { Person2.dao.findAllBy(Person2.AGE.notBetween(2, 25)) }
        expectList() { Person2.dao.findAllBy(Person2.AGE.notBetween(2, 100)) }
    }
    @Test fun isTrue() {
        val person = Person2(name = "Foo", age = 25, isAlive25 = false)
        person.save()
        expectList() { Person2.dao.findAllBy(Person2.ISALIVE25.isTrue()) }
        person.isAlive25 = true
        person.save()
        expectList(person) { Person2.dao.findAllBy(Person2.ISALIVE25.isTrue()) }
    }
    @Test fun isFalse() {
        val person = Person2(name = "Foo", age = 25, isAlive25 = false)
        person.save()
        expectList(person) { Person2.dao.findAllBy(Person2.ISALIVE25.isFalse()) }
        person.isAlive25 = true
        person.save()
        expectList() { Person2.dao.findAllBy(Person2.ISALIVE25.isFalse()) }
    }
    @Test fun equalIgnoreCase() {
        val person = Person2(name = "Foo", age = 25, isAlive25 = true)
        person.save()
        expectList() { Person2.dao.findAllBy(Person2.NAME.equalIgnoreCase("Bar")) }
        expectList(person) { Person2.dao.findAllBy(Person2.NAME.equalIgnoreCase("foo")) }
        expectList(person) { Person2.dao.findAllBy(Person2.NAME.equalIgnoreCase("Foo")) }
    }
    @Test fun notEqualIgnoreCase() {
        val person = Person2(name = "Foo", age = 25, isAlive25 = true)
        person.save()
        expectList(person) { Person2.dao.findAllBy(Person2.NAME.notEqualIgnoreCase("Bar")) }
        expectList() { Person2.dao.findAllBy(Person2.NAME.notEqualIgnoreCase("foo")) }
        expectList() { Person2.dao.findAllBy(Person2.NAME.notEqualIgnoreCase("Foo")) }
    }
    @Test fun like() {
        val person = Person2(name = "Foo", age = 25, isAlive25 = true)
        person.save()
        expectList() { Person2.dao.findAllBy(Person2.NAME.like("%Bar")) }
        // MariaDB matches case-insensitive, H2 does not.
//        expectList() { Person2.dao.findAllBy(Person2.NAME.like("%foo")) }
        expectList(person) { Person2.dao.findAllBy(Person2.NAME.like("%Foo")) }
        expectList(person) { Person2.dao.findAllBy(Person2.NAME.like("Foo")) }
        expectList() { Person2.dao.findAllBy(Person2.NAME.like("o")) }
    }
    @Test fun likeIgnoreCase() {
        val person = Person2(name = "Foo", age = 25, isAlive25 = true)
        person.save()
        expectList() { Person2.dao.findAllBy(Person2.NAME.likeIgnoreCase("%Bar")) }
        expectList(person) { Person2.dao.findAllBy(Person2.NAME.likeIgnoreCase("%foo")) }
        expectList(person) { Person2.dao.findAllBy(Person2.NAME.likeIgnoreCase("%Foo")) }
        expectList(person) { Person2.dao.findAllBy(Person2.NAME.likeIgnoreCase("Foo")) }
        expectList(person) { Person2.dao.findAllBy(Person2.NAME.likeIgnoreCase("foo")) }
        expectList() { Person2.dao.findAllBy(Person2.NAME.likeIgnoreCase("o")) }
    }
    @Test fun isNull() {
        val person = Person2(name = "Foo", age = 25)
        person.save()
        expectList(person) { Person2.dao.findAllBy(Person2.ISALIVE25.isNull()) }
        person.isAlive25 = true
        person.save()
        expectList() { Person2.dao.findAllBy(Person2.ISALIVE25.isNull()) }
    }
    @Test fun isNotNull() {
        val person = Person2(name = "Foo", age = 25)
        person.save()
        expectList() { Person2.dao.findAllBy(Person2.ISALIVE25.isNotNull()) }
        person.isAlive25 = true
        person.save()
        expectList(person) { Person2.dao.findAllBy(Person2.ISALIVE25.isNotNull()) }
    }
    @Nested inner class NoConditionTests {
        @Test fun findAllBy() {
            val person = Person2(name = "Foo", age = 25)
            person.save()
            expectList(person) { Person2.dao.findAllBy(null) }
            expectList(person) { Person2.dao.findAllBy(Condition.NO_CONDITION) }
        }
        @Test fun deleteAllByNull() {
            val person = Person2(name = "Foo", age = 25)
            person.save()
            Person2.dao.deleteBy(null)
            expectList() { Person2.dao.findAll() }
        }
        @Test fun deleteAllByNoCondition() {
            val person = Person2(name = "Foo", age = 25)
            person.save()
            Person2.dao.deleteBy(Condition.NO_CONDITION)
            expectList() { Person2.dao.findAll() }
        }
        @Test fun existsBy() {
            expect(false) { Person2.dao.existsBy(null) }
            expect(false) { Person2.dao.existsBy(Condition.NO_CONDITION) }
            val person = Person2(name = "Foo", age = 25)
            person.save()
            expect(true) { Person2.dao.existsBy(null) }
            expect(true) { Person2.dao.existsBy(Condition.NO_CONDITION) }
        }
        @Test fun countBy() {
            expect(0) { Person2.dao.countBy(null) }
            expect(0) { Person2.dao.countBy(Condition.NO_CONDITION) }
            val person = Person2(name = "Foo", age = 25)
            person.save()
            expect(1) { Person2.dao.countBy(null) }
            expect(1) { Person2.dao.countBy(Condition.NO_CONDITION) }
        }
    }
    @Test fun coalesce() {
        Person2(name = "Foo", age = 25).save()
        expect(1) { Person2.dao.countBy(Person2.SOMESTRINGVALUE.coalesce("Foo").eq("Foo")) }
        Person2(name = "Foo", age = 25, someStringValue = "Bar").save()
        expect(1) { Person2.dao.countBy(Person2.SOMESTRINGVALUE.coalesce("Foo").eq("Foo")) }
        expect(1) { Person2.dao.countBy(Person2.SOMESTRINGVALUE.coalesce("Foo").eq("Bar")) }
        expect(0) { Person2.dao.countBy(Person2.SOMESTRINGVALUE.coalesce("Foo").eq("Baz")) }
    }
    @Test fun nullIf() {
        Person2(name = "Foo", age = 25).save()
        expect(1) { Person2.dao.countBy(Person2.SOMESTRINGVALUE.nullIf("Foo").isNull()) }
        expect(1) { Person2.dao.countBy(Person2.NAME.nullIf("Foo").isNull()) }
        expect(1) { Person2.dao.countBy(Person2.NAME.nullIf("Bar").eq("Foo")) }
    }
    @Test fun cast() {
        Person2(name = "Foo", age = 25).save()
        expect(0) { Person2.dao.countBy(Person2.SOMESTRINGVALUE.castAsVarchar().eq("Foo")) }
        expect(1) { Person2.dao.countBy(Person2.NAME.castAsVarchar().eq("Foo")) }
        expect(1) { Person2.dao.countBy(Person2.AGE.castAsVarchar().eq("25")) }
    }
    @Test fun ifNull() {
        Assumptions.assumeTrue(dbInfo.variant == DatabaseVariant.H2)
        Person2(name = "Foo", age = 25).save()
        expect(1) {
            Person2.dao.countBy(
                Person2.SOMESTRINGVALUE.ifNull("Foo").eq("Foo")
            )
        }
        Person2(name = "Foo", age = 25, someStringValue = "Bar").save()
        expect(1) {
            Person2.dao.countBy(
                Person2.SOMESTRINGVALUE.ifNull("Foo").eq("Foo")
            )
        }
        expect(1) {
            Person2.dao.countBy(
                Person2.SOMESTRINGVALUE.ifNull("Foo").eq("Bar")
            )
        }
        expect(0) {
            Person2.dao.countBy(
                Person2.SOMESTRINGVALUE.ifNull("Foo").eq("Baz")
            )
        }
    }
    @Nested inner class FullTextSearchTests {
        @Test fun constructSqlSucceeds() {
            Person2.NAME.fullTextMatches("foo").toSql()
        }

        @Test fun smokeTest() {
            Assumptions.assumeTrue(dbInfo.supportsFullText)
            Person.findAllBy(Person.NAME.fullTextMatches(""))
            Person.findAllBy(Person.NAME.fullTextMatches("a"))
            Person.findAllBy(Person.NAME.fullTextMatches("the"))
            Person.findAllBy(Person.NAME.fullTextMatches("Moby"))
        }

        @Test fun `blank filter matches all records`() {
            Assumptions.assumeTrue(dbInfo.supportsFullText)
            val moby = Person(name = "Moby")
            moby.create()
            expectList(moby) { Person.findAllBy(Person.NAME.fullTextMatches("")) }
        }

        @Test fun `various queries matching not matching Moby`() {
            Assumptions.assumeTrue(dbInfo.supportsFullText)
            val moby = Person(name = "Moby")
            moby.create()
            expectList() { Person.findAllBy(Person.NAME.fullTextMatches("foobar")) }
            expectList(moby) { Person.findAllBy(Person.NAME.fullTextMatches("Moby")) }
            expectList() { Person.findAllBy(Person.NAME.fullTextMatches("Jerry")) }
            expectList() { Person.findAllBy(Person.NAME.fullTextMatches("Jerry Moby")) }
        }

        @Test fun partialMatch() {
            Assumptions.assumeTrue(dbInfo.supportsFullText)
            val moby = Person(name = "Moby")
            moby.create()
            expectList(moby) { Person.findAllBy(Person.NAME.fullTextMatches("Mob")) }
        }
    }
}
