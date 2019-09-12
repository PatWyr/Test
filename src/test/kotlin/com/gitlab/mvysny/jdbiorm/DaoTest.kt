package com.gitlab.mvysny.jdbiorm

import com.github.mvysny.dynatest.DynaTest
import com.github.mvysny.dynatest.expectThrows
import java.time.Instant
import java.time.LocalDate
import java.util.*
import kotlin.test.expect

class DaoTest : DynaTest({
    withAllDatabases {
        group("Person") {
            test("FindById") {
                expect(null) { Person.findById(25) }
                val p = Person(name = "Albedo", age = 130)
                p.save()
                p.modified = p.modified!!.withZeroNanos
                expect(p) { Person.findById(p.id!!) }
            }
            test("GetById") {
                val p = Person(name = "Albedo", age = 130)
                p.save()
                p.modified = p.modified!!.withZeroNanos
                expect(p) { Person.getById(p.id!!) }
            }
            test("GetById fails if there is no such entity") {
                expectThrows(IllegalArgumentException::class, message = "There is no Person for id 25") {
                    Person.getById(25L)
                }
            }
            group("getBy() tests") {
                test("succeeds if there is exactly one matching entity") {
                    val p = Person(name = "Albedo", age = 130)
                    p.save()
                    expect(p.withZeroNanos()) { Person.getOneBy("name = :name") { it.bind("name", "Albedo") } }
                }

                test("fails if there is no such entity") {
                    expectThrows(IllegalArgumentException::class, message = "no Person satisfying 'name = :name'{positional:{}, named:{name:Albedo}, finder:[]}") {
                        Person.getOneBy("name = :name") { it.bind("name", "Albedo") }
                    }
                }

                test("fails if there are two matching entities") {
                    repeat(2) { Person(name = "Albedo", age = 130).save() }
                    expectThrows(IllegalArgumentException::class, message = "too many Person satisfying 'name = :name'{positional:{}, named:{name:Albedo}, finder:[]}") {
                        Person.getOneBy("name = :name") { it.bind("name", "Albedo") }
                    }
                }

                test("fails if there are ten matching entities") {
                    repeat(10) { Person(name = "Albedo", age = 130).save() }
                    expectThrows(IllegalArgumentException::class, message = "too many Person satisfying 'name = :name'{positional:{}, named:{name:Albedo}, finder:[]}") {
                        Person.getOneBy("name = :name") { it.bind("name", "Albedo") }
                    }
                }
            }
        }
    }
})
