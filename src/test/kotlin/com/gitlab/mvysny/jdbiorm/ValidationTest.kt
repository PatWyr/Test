package com.gitlab.mvysny.jdbiorm

import com.github.mvysny.dynatest.DynaTest
import com.github.mvysny.dynatest.expectThrows
import com.gitlab.mvysny.jdbiorm.JdbiOrm.jdbi
import javax.validation.ValidationException
import kotlin.test.expect
import kotlin.test.fail

class ValidationTest : DynaTest({
    usingH2Database()
    test("Validation on empty name fails") {
        expectThrows(ValidationException::class) {
            Person(name = "", age = 20).validate()
        }
        expect(false) { Person(name = "", age = 20).isValid() }
    }
    test("Validation on non-empty name succeeds") {
        Person(name = "Valid Name", age = 20).validate()
        expect(true) { Person(name = "Valid Name", age = 20).isValid() }
    }
    test("save() fails when the bean is invalid") {
        expectThrows(ValidationException::class, "name: length must be between 1 and 2147483647") {
            Person(name = "", age = 20).save()
        }
    }
    test("validation is skipped when save(false) is called") {
        data class ValidationAlwaysFails(private var id: Long?) : Entity<Long> {
            override fun getId(): Long? = id
            override fun setId(id: Long?) { this.id = id }
            override fun validate() = fail("Shouldn't be called")
        }
        jdbi().useHandle<Exception> {
            it.createUpdate("create table ValidationAlwaysFails ( id bigint primary key auto_increment )")
                    .execute()
        }
        ValidationAlwaysFails(null).save(false)
    }
})
