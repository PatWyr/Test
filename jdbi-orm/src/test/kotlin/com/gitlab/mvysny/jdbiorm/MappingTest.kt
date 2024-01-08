@file:Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")

package com.gitlab.mvysny.jdbiorm

import com.github.mvysny.dynatest.*
import java.lang.IllegalStateException
import java.lang.Long
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoField
import java.util.*
import kotlin.test.expect

@DynaTestDsl
fun DynaNodeGroup.dbMappingTests() {
    group("Person") {
        personTestBattery()
    }
    group("EntityWithAliasedId") {
        aliasedIdTestBattery()
    }
    group("NaturalPerson") {
        naturalPersonTests()
    }
    group("LogRecord") {
        logRecordTestBattery()
    }
    group("Composite PK") {
        compositePKTestBattery()
    }
    group("TypeMapping") {
        test("java enum to native db enum") {
            for (it in MaritalStatus.values().plusNull) {
                val id: kotlin.Long? =
                    TypeMappingEntity(enumTest = it).run { save(); id }
                val loaded = TypeMappingEntity.findById(id!!)!!
                expect(it) { loaded.enumTest }
            }
        }
    }
    test("custom select") {
        Person(name = "Albedo", age = 130).save()
        expectList("Albedo") { Person.dao.findAll2().map { it.name } }
    }
}

@DynaTestDsl
private fun DynaNodeGroup.personTestBattery() {
    test("FindAll") {
        expectList() { Person.findAll() }
        val p = Person(name = "Zaphod", age = 42, ignored2 = Object())
        p.save()
        expect(true) { p.id != null }
        p.ignored2 = null
        expectList(p.withZeroNanos()) { Person.findAll().map { it.withZeroNanos() } }
    }
    group("save") {
        test("Save") {
            val p = Person(name = "Albedo", age = 130)
            p.save()
            expectList("Albedo") { Person.findAll().map { it.name } }
            p.name = "Rubedo"
            p.save()
            expectList("Rubedo") { Person.findAll().map { it.name } }
            Person(name = "Nigredo", age = 130).save()
            expectList("Rubedo", "Nigredo") { Person.findAll().map { it.name } }
        }
        test("SaveEnum") {
            val p = Person2(name = "Zaphod", age = 42, maritalStatus = MaritalStatus.Divorced)
            p.save()
            class Foo(var maritalStatus: String? = null) {
                constructor(): this(null)
            }
            expectList("Divorced") {
                db {
                    createQuery("select maritalStatus from Test").mapToBean(Foo::class.java).list().map { it.maritalStatus }
                }
            }
            expect(p) { db { Person2.findAll()[0] } }
        }
        test("SaveLocalDate") {
            val p = Person(name = "Zaphod", age = 42, dateOfBirth = LocalDate.of(1990, 1, 14))
            p.save()
            expect(LocalDate.of(1990, 1, 14)) { db { Person.findAll()[0].dateOfBirth!! } }
        }
        test("save date and instant") {
            val p = Person(name = "Zaphod", age = 20, created = Date(1000), modified = Instant.ofEpochMilli(120398123))
            p.save()
            expect(1000) { db { Person.findAll()[0].created!!.time } }
            expect(Instant.ofEpochMilli(120398123)) { db { Person.findAll()[0].modified!! } }
        }
        test("updating non-existing row fails") {
            val p = Person(id = 15, name = "Zaphod", age = 20, created = Date(1000), modified = Instant.ofEpochMilli(120398123))
            expectThrows(IllegalStateException::class, "We expected to update only one row but we updated 0 - perhaps there is no row with id 15?") {
                p.save()
            }
        }
    }
    test("delete") {
        val p = Person(name = "Albedo", age = 130)
        p.save()
        p.delete()
        expectList() { Person.findAll() }
    }
    test("JsonSerializationIgnoresMeta") {
        expect("""{"name":"Zaphod","age":42}""") { gson.toJson(Person(name = "Zaphod", age = 42)) }
    }
    test("Meta") {
        val meta = Person.meta
        expect("Test") { meta.databaseTableName }  // since Person is annotated with @Entity("Test")
        expect(1) { meta.idProperty.size }
        expect("Test.id") { meta.idProperty[0].dbName.qualifiedName }
        expect(Person::class.java) { meta.entityClass }
        expect(java.lang.Long::class.java) { meta.idProperty[0].valueType }
        expect(setOf("Test.id", "Test.name", "Test.age", "Test.dateOfBirth", "Test.created", "Test.alive", "Test.maritalStatus", "Test.modified")) {
            meta.persistedFieldDbNames.map { it.qualifiedName } .toSet()
        }
    }
    test("reload") {
        val p = Person(name = "Albedo", age = 130)
        p.save()
        p.age = 25
        p.name = "Nigredo"
        p.reload()
        expect("Albedo") { p.name }
        expect(130) { p.age }
    }
}

@DynaTestDsl
private fun DynaNodeGroup.aliasedIdTestBattery() {
    test("FindAll") {
        expectList() { EntityWithAliasedId.dao.findAll() }
        val p = EntityWithAliasedId("Zaphod")
        p.save()
        expect(true) { p.id != null }
        expectList("Zaphod") { EntityWithAliasedId.dao.findAll().map { it.name } }
    }
    test("Save") {
        val p = EntityWithAliasedId(null, "Albedo")
        p.save()
        expectList("Albedo") { EntityWithAliasedId.dao.findAll().map { it.name } }
        p.name = "Rubedo"
        p.save()
        expectList("Rubedo") { EntityWithAliasedId.dao.findAll().map { it.name } }
        EntityWithAliasedId(null, "Nigredo").save()
        expectList("Rubedo", "Nigredo") { EntityWithAliasedId.dao.findAll().map { it.name } }
    }
    test("delete") {
        val p = EntityWithAliasedId(null, "Albedo")
        p.save()
        p.delete()
        expect(listOf()) { EntityWithAliasedId.dao.findAll() }
    }
    test("JsonSerializationIgnoresMeta") {
        expect("""{"name":"Zaphod"}""") { gson.toJson(EntityWithAliasedId(null, "Zaphod")) }
    }
    test("Meta") {
        val meta = EntityMeta.of(EntityWithAliasedId::class.java)
        expect("EntityWithAliasedId") { meta.databaseTableName }
        expect(1) { meta.idProperty.size }
        expect("EntityWithAliasedId.myid") { meta.idProperty[0].dbName.qualifiedName }
        expect(EntityWithAliasedId::class.java) { meta.entityClass }
        expect(Long::class.java) { meta.idProperty[0].valueType }
        expect(setOf("myid", "name")) { meta.persistedFieldDbNames.map { it.unqualifiedName } .toSet() }
    }
}

@DynaTestDsl
private fun DynaNodeGroup.compositePKTestBattery() {
    test("FindAll") {
        expectList() { MappingTable.dao.findAll() }
        val p = MappingTable(1, 2, "Zaphod")
        p.create()
        expect(true) { p.id != null }
        expectList("MappingTable{id=1, 2, Zaphod}") { MappingTable.dao.findAll().map { it.toString() } }
    }
    test("Save") {
        val p = MappingTable(1, 2, "Albedo")
        p.create()
        expectList("MappingTable{id=1, 2, Albedo}") { MappingTable.dao.findAll().map { it.toString() } }
        p.someData = "Rubedo"
        p.save()
        expectList("Rubedo") { MappingTable.dao.findAll().map { it.someData } }
        MappingTable(1, 3, "Nigredo").create()
        expectList("Rubedo", "Nigredo") { MappingTable.dao.findAll().map { it.someData } }
    }
    test("delete") {
        val p = MappingTable(1, 2, "Albedo")
        p.create()
        p.delete()
        expect(listOf()) { MappingTable.dao.findAll() }
    }
    test("Meta") {
        val meta = EntityMeta.of(MappingTable::class.java)
        expect("mapping_table") { meta.databaseTableName }
        expect(2) { meta.idProperty.size }
        expect("mapping_table.person_id") { meta.idProperty[0].dbName.qualifiedName }
        expect("mapping_table.department_id") { meta.idProperty[1].dbName.qualifiedName }
        expect(MappingTable::class.java) { meta.entityClass }
        expect(Long::class.java) { meta.idProperty[0].valueType }
        expect(Long::class.java) { meta.idProperty[1].valueType }
        expect(setOf("person_id", "department_id", "some_data")) { meta.persistedFieldDbNames.map { it.unqualifiedName } .toSet() }
    }
    test("reload") {
        val p = MappingTable(1, 2, "Albedo")
        p.create()
        p.someData = "Foo"
        p.reload()
        expect("Albedo") { p.someData }
        expect(2) { p.id!!.departmentId }
        expect(1) { p.id!!.personId }
    }
}

@DynaTestDsl
private fun DynaNodeGroup.naturalPersonTests() {
    test("save fails") {
        val p = NaturalPerson(id = "12345678", name = "Albedo", bytes = byteArrayOf(5))
        expectThrows<IllegalStateException>("We expected to update only one row but we updated 0 - perhaps there is no row with id 12345678?") {
            p.save()
        }
    }
    test("Save") {
        val p = NaturalPerson(id = "12345678", name = "Albedo", bytes = byteArrayOf(5))
        p.create()
        expectList("Albedo") { NaturalPerson.findAll().map { it.name } }
        p.name = "Rubedo"
        p.save()
        expectList("Rubedo") { NaturalPerson.findAll().map { it.name } }
        NaturalPerson(id = "aaa", name = "Nigredo", bytes = byteArrayOf(5)).create()
        expectList("Rubedo", "Nigredo") { NaturalPerson.findAll().map { it.name } }
    }
    test("delete") {
        val p = NaturalPerson(id = "foo", name = "Albedo", bytes = byteArrayOf(5))
        p.create()
        p.delete()
        expectList() { NaturalPerson.findAll() }
    }
    test("reload") {
        val p = NaturalPerson(id = "foo", name = "Albedo")
        p.create()
        p.name = "Nigredo"
        p.reload()
        expect("Albedo") { p.name }
        expect("foo") { p.id }
    }
}

@DynaTestDsl
private fun DynaNodeGroup.logRecordTestBattery() {
    test("save succeeds since create() auto-generates ID") {
        val p = LogRecord(text = "foo")
        // [id] is mapped to UUID in MySQL which is binary(16). This tests
        // that a converter kicks in.
        p.save()
        expectList("foo") { LogRecord.findAll().map { it.text } }
    }
    test("Save") {
        val p = LogRecord(text = "Albedo")
        p.save()
        expectList("Albedo") { LogRecord.findAll().map { it.text } }
        p.text = "Rubedo"
        p.save()
        expectList("Rubedo") { LogRecord.findAll().map { it.text } }
        LogRecord(text = "Nigredo").save()
        expect(setOf("Rubedo", "Nigredo")) { LogRecord.findAll().map { it.text } .toSet() }
    }
    test("delete") {
        val p = LogRecord(text = "foo")
        p.save()
        p.delete()
        expectList() { LogRecord.findAll() }
    }
    test("reload") {
        val p = LogRecord(text = "foo")
        expect(null) { p.id }
        p.save()
        val id = p.id!!
        p.text = "bar"
        p.reload()
        expect("foo") { p.text }
        expect(id) { p.id }
    }
}

val Instant.withZeroNanos: Instant get() = with(ChronoField.NANO_OF_SECOND, get(ChronoField.MILLI_OF_SECOND).toLong() * 1000000)
// MSSQL nulls out millis for some reason when running on CI
val Date.withZeroMillis: Date get() {
    val result = Timestamp((this as Timestamp).time / 1000 * 1000)
    result.nanos = 0
    return result
}
val <T> Array<T>.plusNull: List<T?> get() = toList<T?>() + listOf(null)
