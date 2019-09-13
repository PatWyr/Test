package com.gitlab.mvysny.jdbiorm

import com.github.mvysny.dynatest.DynaNodeGroup
import com.github.mvysny.dynatest.DynaTest
import com.github.mvysny.dynatest.expectList
import com.github.mvysny.dynatest.expectThrows
import java.lang.IllegalStateException
import java.time.Instant
import java.time.LocalDate
import java.util.*
import kotlin.test.expect

class DaoTest : DynaTest({
    withAllDatabases {
        group("Person") {
            personTestSuite()
        }

        // quick tests which test that DAO methods generally work with entities with aliased ID columns
        group("EntityWithAliasedId") {
            entityWithAliasedIdTestSuite()
        }
    }
})

private fun DynaNodeGroup.personTestSuite() {
    group("findAll") {
        test("no rows returned on empty table") {
            expectList() { Person.findAll() }
        }
        test("all rows returned") {
            db { (0..300).forEach { Person(name = "Albedo", age = it).save() } }
            expect((0..300).toList()) { Person.findAll().map { it.age } .sorted() }
        }
        test("empty paging") {
            db { (0..100).forEach { Person(name = "Albedo", age = it).save() } }
            expectList() { Person.findAll(0L, 0L) }
            expectList() { Person.findAll(20L, 0L) }
            expectList() { Person.findAll(2000L, 0L) }
        }
        test("paging") {
            db { (0..100).forEach { Person(name = "Albedo", age = it).save() } }
            expect((0..9).toList()) { Person.findAll(0L, 10L).map { it.age } }
            expect((20..49).toList()) { Person.findAll(20L, 30L).map { it.age } }
            expect((90..100).toList()) { Person.findAll(90L, 300L).map { it.age } }
            expectList() { Person.findAll(2000L, 50L) }
        }
        group("findAllBy") {
            test("non-paged") {
                val p = Person(name = "Albedo", age = 130)
                p.save()
                expectList(p.withZeroNanos()) {
                    Person.findAllBy("name = :name", null, null) { it.bind("name", "Albedo") }
                            .map { it.withZeroNanos() }
                }
            }
            test("paged") {
                db { (0..100).forEach { Person(name = "Albedo", age = it).save() } }
                expect((20..30).toList()) {
                    Person.findAllBy("name = :name", 20L, 11L) { it.bind("name", "Albedo") }
                            .map { it.age }
                }
            }
        }
    }
    test("FindById") {
        expect(null) { Person.findById(25) }
        val p = Person(name = "Albedo", age = 130)
        p.save()
        expect(p.withZeroNanos()) { Person.findById(p.id!!)?.withZeroNanos() }
    }
    test("GetById") {
        val p = Person(name = "Albedo", age = 130)
        p.save()
        expect(p.withZeroNanos()) { Person.getById(p.id!!).withZeroNanos() }
    }
    test("GetById fails if there is no such entity") {
        expectThrows(IllegalStateException::class, message = "There is no Person for id 25") {
            Person.getById(25L)
        }
    }
    group("getBy() tests") {
        test("succeeds if there is exactly one matching entity") {
            val p = Person(name = "Albedo", age = 130)
            p.save()
            expect(p.withZeroNanos()) {
                Person.getOneBy("name = :name") { it.bind("name", "Albedo") } .withZeroNanos()
            }
        }

        test("fails if there is no such entity") {
            expectThrows(IllegalStateException::class, message = "no row matching Person: 'name = :name'{positional:{}, named:{name:Albedo}, finder:[]}") {
                Person.getOneBy("name = :name") { it.bind("name", "Albedo") }
            }
        }

        test("fails if there are two matching entities") {
            repeat(2) { Person(name = "Albedo", age = 130).save() }
            expectThrows(IllegalStateException::class, message = "too many rows matching Person: 'name = :name'{positional:{}, named:{name:Albedo}, finder:[]}") {
                Person.getOneBy("name = :name") { it.bind("name", "Albedo") }
            }
        }

        test("fails if there are ten matching entities") {
            repeat(10) { Person(name = "Albedo", age = 130).save() }
            expectThrows(IllegalStateException::class, message = "too many rows matching Person: 'name = :name'{positional:{}, named:{name:Albedo}, finder:[]}") {
                Person.getOneBy("name = :name") { it.bind("name", "Albedo") }
            }
        }
    }
    group("count") {
        test("basic count") {
            expect(0) { Person.count() }
            listOf("Albedo", "Nigredo", "Rubedo").forEach { Person(name = it, age = 130).save() }
            expect(3) { Person.count() }
        }
        test("count with filters") {
            expect(0) { Person.countBy("age > :age") { q -> q.bind("age", 6) } }
            listOf("Albedo", "Nigredo", "Rubedo").forEach { Person(name = it, age = it.length).save() }
            expect(1) { Person.countBy("age > :age") { q -> q.bind("age", 6) } }
        }
    }
    test("DeleteAll") {
        listOf("Albedo", "Nigredo", "Rubedo").forEach { Person(name = it, age = 130).save() }
        expect(3) { Person.count() }
        Person.deleteAll()
        expect(0) { Person.count() }
    }
    group("DeleteById") {
        test("simple") {
            listOf("Albedo", "Nigredo", "Rubedo").forEach { Person(name = it, age = 130).save() }
            expect(3) { Person.count() }
            Person.deleteById(Person.findAll().first { it.name == "Albedo" }.id!!)
            expect(listOf("Nigredo", "Rubedo")) { Person.findAll().map { it.name } }
        }
        test("DoesNothingOnUnknownId") {
            db { Person.deleteById(25L) }
            expect(listOf()) { Person.findAll() }
        }
    }
    test("DeleteBy") {
        listOf("Albedo", "Nigredo", "Rubedo").forEach { Person(name = it, age = 130).save() }
        Person.deleteBy("name = :name") { q -> q.bind("name", "Albedo") }
        expect(listOf("Nigredo", "Rubedo")) { Person.findAll().map { it.name } }
    }
    group("findOneBy() tests") {
        test("succeeds if there is exactly one matching entity") {
            val p = Person(name = "Albedo", age = 130)
            p.save()
            expect(p.withZeroNanos()) {
                Person.findOneBy("name = :name") { it.bind("name", "Albedo") } ?.withZeroNanos()
            }
        }

        test("returns null if there is no such entity") {
            expect(null) { Person.findOneBy("name = :name") { it.bind("name", "Albedo") } }
        }

        test("fails if there are two matching entities") {
            repeat(2) { Person(name = "Albedo", age = 130).save() }
            expectThrows(IllegalStateException::class, "too many rows matching Person: 'name = :name'{positional:{}, named:{name:Albedo}, finder:[]}") {
                Person.findOneBy("name = :name") { it.bind("name", "Albedo") }
            }
        }

        test("fails if there are ten matching entities") {
            repeat(10) { Person(name = "Albedo", age = 130).save() }
            expectThrows(IllegalStateException::class, "too many rows matching Person: 'name = :name'{positional:{}, named:{name:Albedo}, finder:[]}") {
                Person.findOneBy("name = :name") { it.bind("name", "Albedo") }
            }
        }

        test("test filter by date") {
            val p = Person(name = "Albedo", age = 130, dateOfBirth = LocalDate.of(1980, 2, 2))
            p.save()
            expect(p.withZeroNanos()) {
                Person.findOneBy("dateOfBirth = :dateOfBirth") { q -> q.bind("dateOfBirth", LocalDate.of(1980, 2, 2)) } ?.withZeroNanos()
            }
            // here I don't care about whether it selects something or not, I'm only testing the database compatibility
            Person.findOneBy("dateOfBirth = :dateOfBirth") { q -> q.bind("dateOfBirth", Instant.now()) }
            Person.findOneBy("dateOfBirth = :dateOfBirth") { q -> q.bind("dateOfBirth", Date()) }
        }
    }
    group("exists") {
        test("returns false on empty table") {
            expect(false) { Person.existsAny() }
            expect(false) { Person.existsById(25) }
            expect(false) { Person.existsBy("age=:age") { it.bind("age", 26) } }
        }
        test("returns true on matching entity") {
            val p = Person(name = "Albedo", age = 130)
            p.save()
            p.modified = p.modified!!.withZeroNanos
            expect(true) { Person.existsAny() }
            expect(true) { Person.existsById(p.id!!) }
            expect(true) { Person.existsBy("age>=:age") { it.bind("age", 26) } }
        }
        test("returns true on non-matching entity") {
            val p = Person(name = "Albedo", age = 130)
            p.save()
            p.modified = p.modified!!.withZeroNanos
            expect(true) { Person.existsAny() }
            expect(false) { Person.existsById(p.id!! + 1) }
            expect(false) { Person.existsBy("age<=:age") { it.bind("age", 26) } }
        }
    }
}

private fun DynaNodeGroup.entityWithAliasedIdTestSuite() {
    test("FindById") {
        expect(null) { EntityWithAliasedId.dao.findById(25) }
        val p = EntityWithAliasedId("Albedo")
        p.save()
        expect(p) { EntityWithAliasedId.dao.findById(p.id!!) }
    }
    test("GetById") {
        val p = EntityWithAliasedId("Albedo")
        p.save()
        expect(p) { EntityWithAliasedId.dao.getById(p.id!!) }
    }
    group("getBy() tests") {
        test("succeeds if there is exactly one matching entity") {
            val p = EntityWithAliasedId("Albedo")
            p.save()
            expect(p) { EntityWithAliasedId.dao.getOneBy("name=:name") { it.bind("name", "Albedo") } }
        }
    }
    group("count") {
        test("basic count") {
            expect(0) { EntityWithAliasedId.dao.count() }
            listOf("Albedo", "Nigredo", "Rubedo").forEach { EntityWithAliasedId(it).save() }
            expect(3) { EntityWithAliasedId.dao.count() }
        }
        test("count with filters") {
            expect(0) { EntityWithAliasedId.dao.count() }
            listOf("Albedo", "Nigredo", "Rubedo").forEach { EntityWithAliasedId(it).save() }
            expect(1) { EntityWithAliasedId.dao.countBy("name=:name") { it.bind("name", "Albedo") } }
            val id = EntityWithAliasedId.dao.findAll().first { it.name == "Albedo" }.id!!
            expect(1) { EntityWithAliasedId.dao.countBy("myid=:id") { it.bind("id", id) } }
        }
    }
    test("DeleteAll") {
        listOf("Albedo", "Nigredo", "Rubedo").forEach { EntityWithAliasedId(it).save() }
        expect(3) { EntityWithAliasedId.dao.count() }
        EntityWithAliasedId.dao.deleteAll()
        expect(0) { EntityWithAliasedId.dao.count() }
    }
    test("DeleteById") {
        listOf("Albedo", "Nigredo", "Rubedo").forEach { EntityWithAliasedId(it).save() }
        expect(3) { EntityWithAliasedId.dao.count() }
        EntityWithAliasedId.dao.deleteById(EntityWithAliasedId.dao.findAll().first { it.name == "Albedo" }.id!!)
        expect(listOf("Nigredo", "Rubedo")) { EntityWithAliasedId.dao.findAll().map { it.name } }
    }
    test("DeleteByIdDoesNothingOnUnknownId") {
        EntityWithAliasedId.dao.deleteById(25L)
        expect(listOf()) { EntityWithAliasedId.dao.findAll() }
    }
    test("DeleteBy") {
        listOf("Albedo", "Nigredo", "Rubedo").forEach { EntityWithAliasedId(it).save() }
        EntityWithAliasedId.dao.deleteBy("name = :name") { it.bind("name", "Albedo") }  // raw sql where
        expect(listOf("Nigredo", "Rubedo")) { EntityWithAliasedId.dao.findAll().map { it.name } }
    }
    group("findSpecificBy() tests") {
        test("succeeds if there is exactly one matching entity") {
            val p = EntityWithAliasedId("Albedo")
            p.save()
            expect(p) { EntityWithAliasedId.dao.findOneBy("name=:name") { it.bind("name", "Albedo") } }
            expect(p) { EntityWithAliasedId.dao.findOneBy("myid=:id") { it.bind("id", p.id!!) } }
        }
    }
    group("exists") {
        test("returns false on empty table") {
            expect(false) { EntityWithAliasedId.dao.existsAny() }
            expect(false) { EntityWithAliasedId.dao.existsById(25) }
            expect(false) { EntityWithAliasedId.dao.existsBy("name<=:name") { it.bind("name", "a") } }
        }
        test("returns true on matching entity") {
            val p = EntityWithAliasedId("Albedo")
            p.save()
            expect(true) { EntityWithAliasedId.dao.existsAny() }
            expect(true) { EntityWithAliasedId.dao.existsById(p.id!!) }
            expect(true) { EntityWithAliasedId.dao.existsBy("name=:name") { it.bind("name", "Albedo") } }
            expect(true) { EntityWithAliasedId.dao.existsBy("myid=:id") { it.bind("id", p.id!!) } }
        }
    }
}
