package com.gitlab.mvysny.jdbiorm

import com.github.mvysny.dynatest.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.lang.IllegalStateException
import java.time.Instant
import java.time.LocalDate
import java.util.*
import kotlin.test.expect

@DynaTestDsl
fun DynaNodeGroup.dbDaoTests() {
    group("Person") {
        personTestSuite()
    }

    // quick tests which test that DAO methods generally work with entities with aliased ID columns
    group("EntityWithAliasedId") {
        entityWithAliasedIdTestSuite()
    }

    group("Composite PK") {
        compositePKTestSuite()
    }
}

abstract class AbstractDbDaoTests {
    @Nested inner class PersonTests : AbstractPersonTests()
    @Nested inner class CompositePKTests : AbstractCompositePKTests()
}

abstract class AbstractPersonTests {
    @Nested inner class FindAllTests {
        @Test fun `no rows returned on empty table`() {
            expectList() { Person.findAll() }
        }
        @Test fun `all rows returned`() {
            db { (0..300).forEach { Person(name = "Albedo", age = it).save() } }
            expect((0..300).toList()) { Person.findAll().map { it.age } .sorted() }
        }
        @Test fun `empty paging`() {
            db { (0..100).forEach { Person(name = "Albedo", age = it).save() } }
            expectList() { Person.findAll(0L, 0L) }
            expectList() { Person.findAll(20L, 0L) }
            expectList() { Person.findAll(2000L, 0L) }
        }
        @Test fun paging() {
            db { (0..100).forEach { Person(name = "Albedo", age = it).save() } }
            expect((0..9).toList()) { Person.findAll(0L, 10L).map { it.age } }
            expect((20..49).toList()) { Person.findAll(20L, 30L).map { it.age } }
            expect((90..100).toList()) { Person.findAll(90L, 300L).map { it.age } }
            expectList() { Person.findAll(2000L, 50L) }
        }
        @Nested inner class FindAllBy {
            @Test fun nonPaged() {
                val p = Person2(name = "Albedo", age = 130)
                p.save()
                expectList(p) {
                    Person2.findAllBy("name = :name", null, null) { it.bind("name", "Albedo") }
                }
            }
            @Test fun paged() {
                db { (0..100).forEach { Person(name = "Albedo", age = it).save() } }
                expect((20..30).toList()) {
                    Person.findAllBy("name = :name", 20L, 11L) { it.bind("name", "Albedo") }
                        .map { it.age }
                }
            }
            @Test fun sorted() {
                db { (0..10).forEach { Person(name = "Albedo", age = it).save() } }
                expect((0..10).toList()) {
                    Person.findAllBy("name = :name", "age ASC") { it.bind("name", "Albedo") }
                        .map { it.age }
                }
                expect((0..10).toList().reversed()) {
                    Person.findAllBy("name = :name", "age DESC") { it.bind("name", "Albedo") }
                        .map { it.age }
                }
                expect((0..10).toList()) {
                    Person.findAllBy(Person.NAME.eq("Albedo"), listOf(Person.AGE.asc()))
                        .map { it.age }
                }
                expect((0..10).toList().reversed()) {
                    Person.findAllBy(Person.NAME.eq("Albedo"), listOf(Person.AGE.desc()))
                        .map { it.age }
                }
            }
        }
    }
    @Test fun findById() {
        expect(null) { Person.findById(25) }
        val p = Person2(name = "Albedo", age = 130)
        p.save()
        expect(p) { Person2.findById(p.id!!) }
    }
    @Test fun getById() {
        val p = Person2(name = "Albedo", age = 130)
        p.save()
        expect(p) { Person2.getById(p.id!!) }
    }
    @Test fun `GetById fails if there is no such entity`() {
        expectThrows<IllegalStateException>("There is no Person for id 25") {
            Person.getById(25L)
        }
    }
    @Nested inner class GetByTests {
        @Test fun `succeeds if there is exactly one matching entity`() {
            val p = Person2(name = "Albedo", age = 130)
            p.save()
            expect(p) {
                Person2.singleBy("name = :name") { it.bind("name", "Albedo") }
            }
        }

        @Test fun `fails if there is no such entity`() {
            expectThrows<IllegalStateException>("no row matching Person: 'name = :name'{positional:{}, named:{name:Albedo}, finder:[]}") {
                Person.singleBy("name = :name") { it.bind("name", "Albedo") }
            }
        }

        @Test fun `fails if there are two matching entities`() {
            repeat(2) { Person(name = "Albedo", age = 130).save() }
            expectThrows<IllegalStateException>("too many rows matching Person: 'name = :name'{positional:{}, named:{name:Albedo}, finder:[]}") {
                Person.singleBy("name = :name") { it.bind("name", "Albedo") }
            }
        }

        @Test fun `fails if there are ten matching entities`() {
            repeat(10) { Person(name = "Albedo", age = 130).save() }
            expectThrows<IllegalStateException>("too many rows matching Person: 'name = :name'{positional:{}, named:{name:Albedo}, finder:[]}") {
                Person.singleBy("name = :name") { it.bind("name", "Albedo") }
            }
        }
    }
    @Nested inner class CountTests {
        @Test fun basicCount() {
            expect(0) { Person.count() }
            listOf("Albedo", "Nigredo", "Rubedo").forEach { Person(name = it, age = 130).save() }
            expect(3) { Person.count() }
        }
        @Test fun countWithFilters() {
            expect(0) { Person.countBy("age > :age") { q -> q.bind("age", 6) } }
            listOf("Albedo", "Nigredo", "Rubedo").forEach { Person(name = it, age = it.length).save() }
            expect(1) { Person.countBy("age > :age") { q -> q.bind("age", 6) } }
        }
        @Test fun countWithCondition() {
            expect(0) { Person.countBy(Person.AGE.gt(6)) }
            listOf("Albedo", "Nigredo", "Rubedo").forEach { Person(name = it, age = it.length).save() }
            expect(1) { Person.countBy(Person.AGE.gt(6)) }
        }
    }
    @Test fun deleteAll() {
        listOf("Albedo", "Nigredo", "Rubedo").forEach { Person(name = it, age = 130).save() }
        expect(3) { Person.count() }
        Person.deleteAll()
        expect(0) { Person.count() }
    }
    @Nested inner class DeleteByIdTests {
        @Test fun simple() {
            listOf("Albedo", "Nigredo", "Rubedo").forEach { Person(name = it, age = 130).save() }
            expect(3) { Person.count() }
            Person.deleteById(Person.findAll().first { it.name == "Albedo" }.id!!)
            expect(listOf("Nigredo", "Rubedo")) { Person.findAll().map { it.name } }
        }
        @Test fun doesNothingOnUnknownId() {
            db { Person.deleteById(25L) }
            expect(listOf()) { Person.findAll() }
        }
    }
    @Test fun deleteBy() {
        listOf("Albedo", "Nigredo", "Rubedo").forEach { Person(name = it, age = 130).save() }
        Person.deleteBy("name = :name") { q -> q.bind("name", "Albedo") }
        expect(listOf("Nigredo", "Rubedo")) { Person.findAll().map { it.name } }
    }
    @Nested inner class FindSingleByTests {
        @Test fun `succeeds if there is exactly one matching entity`() {
            val p = Person(name = "Albedo", age = 130)
            p.save()
            expect(p.withZeroNanos()) {
                Person.findSingleBy("name = :name") { it.bind("name", "Albedo") } ?.withZeroNanos()
            }
        }

        @Test fun `returns null if there is no such entity`() {
            expect(null) { Person.findSingleBy("name = :name") { it.bind("name", "Albedo") } }
        }

        @Test fun `fails if there are two matching entities`() {
            repeat(2) { Person(name = "Albedo", age = 130).save() }
            expectThrows(IllegalStateException::class, "too many rows matching Person: 'name = :name'{positional:{}, named:{name:Albedo}, finder:[]}") {
                Person.findSingleBy("name = :name") { it.bind("name", "Albedo") }
            }
        }

        @Test fun `fails if there are ten matching entities`() {
            repeat(10) { Person(name = "Albedo", age = 130).save() }
            expectThrows(IllegalStateException::class, "too many rows matching Person: 'name = :name'{positional:{}, named:{name:Albedo}, finder:[]}") {
                Person.findSingleBy("name = :name") { it.bind("name", "Albedo") }
            }
        }

        @Test fun `test filter by date`() {
            val p = Person(name = "Albedo", age = 130, dateOfBirth = LocalDate.of(1980, 2, 2))
            p.save()
            expect(p.withZeroNanos()) {
                Person.findSingleBy("dateOfBirth = :dateOfBirth") { q -> q.bind("dateOfBirth", LocalDate.of(1980, 2, 2)) } ?.withZeroNanos()
            }
            // here I don't care about whether it selects something or not, I'm only testing the database compatibility
            Person.findSingleBy("dateOfBirth = :dateOfBirth") { q -> q.bind("dateOfBirth", Instant.now()) }
            Person.findSingleBy("dateOfBirth = :dateOfBirth") { q -> q.bind("dateOfBirth", Date()) }
        }
    }
    @Nested inner class ExistsTests {
        @Test fun `returns false on empty table`() {
            expect(false) { Person.existsAny() }
            expect(false) { Person.existsById(25) }
            expect(false) { Person.existsBy("age=:age") { it.bind("age", 26) } }
        }
        @Test fun `returns true on matching entity`() {
            val p = Person(name = "Albedo", age = 130)
            p.save()
            p.modified = p.modified!!.withZeroNanos
            expect(true) { Person.existsAny() }
            expect(true) { Person.existsById(p.id!!) }
            expect(true) { Person.existsBy("age>=:age") { it.bind("age", 26) } }
        }
        @Test fun `returns true on non-matching entity`() {
            val p = Person(name = "Albedo", age = 130)
            p.save()
            p.modified = p.modified!!.withZeroNanos
            expect(true) { Person.existsAny() }
            expect(false) { Person.existsById(p.id!! + 1) }
            expect(false) { Person.existsBy("age<=:age") { it.bind("age", 26) } }
        }
    }
}

@DynaTestDsl
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
                val p = Person2(name = "Albedo", age = 130)
                p.save()
                expectList(p) {
                    Person2.findAllBy("name = :name", null, null) { it.bind("name", "Albedo") }
                }
            }
            test("paged") {
                db { (0..100).forEach { Person(name = "Albedo", age = it).save() } }
                expect((20..30).toList()) {
                    Person.findAllBy("name = :name", 20L, 11L) { it.bind("name", "Albedo") }
                            .map { it.age }
                }
            }
            test("sorted") {
                db { (0..10).forEach { Person(name = "Albedo", age = it).save() } }
                expect((0..10).toList()) {
                    Person.findAllBy("name = :name", "age ASC") { it.bind("name", "Albedo") }
                        .map { it.age }
                }
                expect((0..10).toList().reversed()) {
                    Person.findAllBy("name = :name", "age DESC") { it.bind("name", "Albedo") }
                        .map { it.age }
                }
                expect((0..10).toList()) {
                    Person.findAllBy(Person.NAME.eq("Albedo"), listOf(Person.AGE.asc()))
                        .map { it.age }
                }
                expect((0..10).toList().reversed()) {
                    Person.findAllBy(Person.NAME.eq("Albedo"), listOf(Person.AGE.desc()))
                        .map { it.age }
                }
            }
        }
    }
    test("FindById") {
        expect(null) { Person.findById(25) }
        val p = Person2(name = "Albedo", age = 130)
        p.save()
        expect(p) { Person2.findById(p.id!!) }
    }
    test("GetById") {
        val p = Person2(name = "Albedo", age = 130)
        p.save()
        expect(p) { Person2.getById(p.id!!) }
    }
    test("GetById fails if there is no such entity") {
        expectThrows<IllegalStateException>("There is no Person for id 25") {
            Person.getById(25L)
        }
    }
    group("getBy() tests") {
        test("succeeds if there is exactly one matching entity") {
            val p = Person2(name = "Albedo", age = 130)
            p.save()
            expect(p) {
                Person2.singleBy("name = :name") { it.bind("name", "Albedo") }
            }
        }

        test("fails if there is no such entity") {
            expectThrows<IllegalStateException>("no row matching Person: 'name = :name'{positional:{}, named:{name:Albedo}, finder:[]}") {
                Person.singleBy("name = :name") { it.bind("name", "Albedo") }
            }
        }

        test("fails if there are two matching entities") {
            repeat(2) { Person(name = "Albedo", age = 130).save() }
            expectThrows<IllegalStateException>("too many rows matching Person: 'name = :name'{positional:{}, named:{name:Albedo}, finder:[]}") {
                Person.singleBy("name = :name") { it.bind("name", "Albedo") }
            }
        }

        test("fails if there are ten matching entities") {
            repeat(10) { Person(name = "Albedo", age = 130).save() }
            expectThrows<IllegalStateException>("too many rows matching Person: 'name = :name'{positional:{}, named:{name:Albedo}, finder:[]}") {
                Person.singleBy("name = :name") { it.bind("name", "Albedo") }
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
        test("count with condition") {
            expect(0) { Person.countBy(Person.AGE.gt(6)) }
            listOf("Albedo", "Nigredo", "Rubedo").forEach { Person(name = it, age = it.length).save() }
            expect(1) { Person.countBy(Person.AGE.gt(6)) }
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
    group("findSingleBy() tests") {
        test("succeeds if there is exactly one matching entity") {
            val p = Person(name = "Albedo", age = 130)
            p.save()
            expect(p.withZeroNanos()) {
                Person.findSingleBy("name = :name") { it.bind("name", "Albedo") } ?.withZeroNanos()
            }
        }

        test("returns null if there is no such entity") {
            expect(null) { Person.findSingleBy("name = :name") { it.bind("name", "Albedo") } }
        }

        test("fails if there are two matching entities") {
            repeat(2) { Person(name = "Albedo", age = 130).save() }
            expectThrows(IllegalStateException::class, "too many rows matching Person: 'name = :name'{positional:{}, named:{name:Albedo}, finder:[]}") {
                Person.findSingleBy("name = :name") { it.bind("name", "Albedo") }
            }
        }

        test("fails if there are ten matching entities") {
            repeat(10) { Person(name = "Albedo", age = 130).save() }
            expectThrows(IllegalStateException::class, "too many rows matching Person: 'name = :name'{positional:{}, named:{name:Albedo}, finder:[]}") {
                Person.findSingleBy("name = :name") { it.bind("name", "Albedo") }
            }
        }

        test("test filter by date") {
            val p = Person(name = "Albedo", age = 130, dateOfBirth = LocalDate.of(1980, 2, 2))
            p.save()
            expect(p.withZeroNanos()) {
                Person.findSingleBy("dateOfBirth = :dateOfBirth") { q -> q.bind("dateOfBirth", LocalDate.of(1980, 2, 2)) } ?.withZeroNanos()
            }
            // here I don't care about whether it selects something or not, I'm only testing the database compatibility
            Person.findSingleBy("dateOfBirth = :dateOfBirth") { q -> q.bind("dateOfBirth", Instant.now()) }
            Person.findSingleBy("dateOfBirth = :dateOfBirth") { q -> q.bind("dateOfBirth", Date()) }
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

@DynaTestDsl
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
            expect(p) { EntityWithAliasedId.dao.singleBy("name=:name") { it.bind("name", "Albedo") } }
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
            expect(p) { EntityWithAliasedId.dao.findSingleBy("name=:name") { it.bind("name", "Albedo") } }
            expect(p) { EntityWithAliasedId.dao.findSingleBy("myid=:id") { it.bind("id", p.id!!) } }
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

@DynaTestDsl
private fun DynaNodeGroup.compositePKTestSuite() {
    test("FindById") {
        expect(null) { MappingTable.dao.findById(MappingTable.ID(1, 2)) }
        val p = MappingTable(1, 2,"Albedo")
        p.create()
        expect(p) { MappingTable.dao.findById(MappingTable.ID(1, 2)) }
    }
    test("GetById") {
        val p = MappingTable(1, 2,"Albedo")
        p.create()
        expect(p) { MappingTable.dao.getById(MappingTable.ID(1, 2)) }
    }
    group("getBy() tests") {
        test("succeeds if there is exactly one matching entity") {
            val p = MappingTable(1, 2,"Albedo")
            p.create()
            expect(p) { MappingTable.dao.singleBy("some_data=:name") { it.bind("name", "Albedo") } }
        }
    }
    group("count") {
        test("basic count") {
            expect(0) { MappingTable.dao.count() }
            MappingTable(1, 1, "Albedo").create()
            MappingTable(2, 2, "Nigredo").create()
            MappingTable(3, 3, "Rubedo").create()
            expect(3) { MappingTable.dao.count() }
        }
        test("count with filters") {
            expect(0) { MappingTable.dao.count() }
            MappingTable(1, 1, "Albedo").create()
            MappingTable(2, 2, "Nigredo").create()
            MappingTable(3, 3, "Rubedo").create()
            expect(1) { MappingTable.dao.countBy("some_data=:name") { it.bind("name", "Albedo") } }
            MappingTable.dao.findAll().first { it.someData == "Albedo" }.id!!
        }
    }
    test("DeleteAll") {
        MappingTable(1, 1, "Albedo").create()
        MappingTable(2, 2, "Nigredo").create()
        MappingTable(3, 3, "Rubedo").create()
        expect(3) { MappingTable.dao.count() }
        MappingTable.dao.deleteAll()
        expect(0) { MappingTable.dao.count() }
    }
    test("DeleteById") {
        MappingTable(1, 1, "Albedo").create()
        MappingTable(2, 2, "Nigredo").create()
        MappingTable(3, 3, "Rubedo").create()
        expect(3) { MappingTable.dao.count() }
        MappingTable.dao.deleteById(MappingTable.ID(1, 1))
        expect(listOf("Nigredo", "Rubedo")) { MappingTable.dao.findAll().map { it.someData } }
    }
    test("DeleteByIdDoesNothingOnUnknownId") {
        MappingTable.dao.deleteById(MappingTable.ID(25, 25))
        expect(listOf()) { MappingTable.dao.findAll() }
    }
    test("DeleteBy") {
        MappingTable(1, 1, "Albedo").create()
        MappingTable(2, 2, "Nigredo").create()
        MappingTable(3, 3, "Rubedo").create()
        MappingTable.dao.deleteBy("some_data = :name") { it.bind("name", "Albedo") }
        expect(listOf("Nigredo", "Rubedo")) { MappingTable.dao.findAll().map { it.someData } }
    }
    group("findSpecificBy() tests") {
        test("succeeds if there is exactly one matching entity") {
            val p = MappingTable(1, 1, "Albedo")
            p.create()
            expect(p) { MappingTable.dao.findSingleBy("some_data=:name") { it.bind("name", "Albedo") } }
        }
    }
    group("exists") {
        test("returns false on empty table") {
            expect(false) { MappingTable.dao.existsAny() }
            expect(false) { MappingTable.dao.existsById(MappingTable.ID(25, 25)) }
            expect(false) { MappingTable.dao.existsBy("some_data<=:name") { it.bind("name", "a") } }
        }
        test("returns true on matching entity") {
            val p = MappingTable(3, 3, "Albedo")
            p.create()
            expect(true) { MappingTable.dao.existsAny() }
            expect(true) { MappingTable.dao.existsById(p.id!!) }
            expect(true) { MappingTable.dao.existsBy("some_data=:name") { it.bind("name", "Albedo") } }
        }
    }
}

abstract class AbstractCompositePKTests {
    @Test fun findById() {
        expect(null) { MappingTable.dao.findById(MappingTable.ID(1, 2)) }
        val p = MappingTable(1, 2,"Albedo")
        p.create()
        expect(p) { MappingTable.dao.findById(MappingTable.ID(1, 2)) }
    }
    @Test fun getById() {
        val p = MappingTable(1, 2,"Albedo")
        p.create()
        expect(p) { MappingTable.dao.getById(MappingTable.ID(1, 2)) }
    }
    @Nested inner class GetByTests {
        @Test fun `succeeds if there is exactly one matching entity`() {
            val p = MappingTable(1, 2,"Albedo")
            p.create()
            expect(p) { MappingTable.dao.singleBy("some_data=:name") { it.bind("name", "Albedo") } }
        }
    }
    @Nested inner class CountTests {
        @Test fun basicCount() {
            expect(0) { MappingTable.dao.count() }
            MappingTable(1, 1, "Albedo").create()
            MappingTable(2, 2, "Nigredo").create()
            MappingTable(3, 3, "Rubedo").create()
            expect(3) { MappingTable.dao.count() }
        }
        @Test fun countWithFilters() {
            expect(0) { MappingTable.dao.count() }
            MappingTable(1, 1, "Albedo").create()
            MappingTable(2, 2, "Nigredo").create()
            MappingTable(3, 3, "Rubedo").create()
            expect(1) { MappingTable.dao.countBy("some_data=:name") { it.bind("name", "Albedo") } }
            MappingTable.dao.findAll().first { it.someData == "Albedo" }.id!!
        }
    }
    @Test fun deleteAll() {
        MappingTable(1, 1, "Albedo").create()
        MappingTable(2, 2, "Nigredo").create()
        MappingTable(3, 3, "Rubedo").create()
        expect(3) { MappingTable.dao.count() }
        MappingTable.dao.deleteAll()
        expect(0) { MappingTable.dao.count() }
    }
    @Test fun deleteById() {
        MappingTable(1, 1, "Albedo").create()
        MappingTable(2, 2, "Nigredo").create()
        MappingTable(3, 3, "Rubedo").create()
        expect(3) { MappingTable.dao.count() }
        MappingTable.dao.deleteById(MappingTable.ID(1, 1))
        expect(listOf("Nigredo", "Rubedo")) { MappingTable.dao.findAll().map { it.someData } }
    }
    @Test fun deleteByIdDoesNothingOnUnknownId() {
        MappingTable.dao.deleteById(MappingTable.ID(25, 25))
        expect(listOf()) { MappingTable.dao.findAll() }
    }
    @Test fun deleteBy() {
        MappingTable(1, 1, "Albedo").create()
        MappingTable(2, 2, "Nigredo").create()
        MappingTable(3, 3, "Rubedo").create()
        MappingTable.dao.deleteBy("some_data = :name") { it.bind("name", "Albedo") }
        expect(listOf("Nigredo", "Rubedo")) { MappingTable.dao.findAll().map { it.someData } }
    }
    @Nested inner class FindSpecificByTests {
        @Test fun `succeeds if there is exactly one matching entity`() {
            val p = MappingTable(1, 1, "Albedo")
            p.create()
            expect(p) { MappingTable.dao.findSingleBy("some_data=:name") { it.bind("name", "Albedo") } }
        }
    }
    @Nested inner class ExistsTests {
        @Test fun `returns false on empty table`() {
            expect(false) { MappingTable.dao.existsAny() }
            expect(false) { MappingTable.dao.existsById(MappingTable.ID(25, 25)) }
            expect(false) { MappingTable.dao.existsBy("some_data<=:name") { it.bind("name", "a") } }
        }
        @Test fun `returns true on matching entity`() {
            val p = MappingTable(3, 3, "Albedo")
            p.create()
            expect(true) { MappingTable.dao.existsAny() }
            expect(true) { MappingTable.dao.existsById(p.id!!) }
            expect(true) { MappingTable.dao.existsBy("some_data=:name") { it.bind("name", "Albedo") } }
        }
    }
}
