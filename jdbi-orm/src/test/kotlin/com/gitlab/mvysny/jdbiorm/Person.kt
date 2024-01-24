package com.gitlab.mvysny.jdbiorm

import com.gitlab.mvysny.jdbiorm.JdbiOrm.jdbi
import org.hibernate.validator.constraints.Length
import org.jdbi.v3.core.annotation.JdbiProperty
import org.jdbi.v3.core.mapper.reflect.ColumnName
import java.time.Instant
import java.time.LocalDate
import java.util.*

/**
 * A test table that tests the most basic cases. The ID is auto-generated by the database.
 */
@Table("Test")
data class Person(
        private var id: Long? = null,
        @field:Length(min = 1)
        var name: String = "",
        var age: Int = -1,
        @field:JdbiProperty(map = false) var ignored: String? = null,
        @Transient var ignored2: Any? = null,
        var dateOfBirth: LocalDate? = null,
        var created: Date? = null,
        var modified: Instant? = null,
    // test of aliased field
        @field:ColumnName("alive")
    var isAlive25: Boolean? = null,
        var maritalStatus: MaritalStatus? = null

) : Entity<Long> {
    override fun getId(): Long? = id
    override fun setId(id: Long?) { this.id = id }

    override fun save(validate: Boolean) {
        if (id == null) {
            if (created == null) created = java.sql.Timestamp(System.currentTimeMillis())
            if (modified == null) modified = Instant.ofEpochMilli(1238123123L)
        }
        super.save(validate)
    }

    // should not be persisted into the database since it's not backed by a field.
    fun getSomeComputedValue(): Int = age + 2

    override fun toString(): String {
        return "Person(id=$id, name='$name', age=$age, ignored=$ignored, ignored2=$ignored2, " +
                "dateOfBirth=$dateOfBirth, created=$created, modified=$modified, " +
                "isAlive25=$isAlive25, maritalStatus=$maritalStatus) type of created: ${created?.javaClass}"
    }

    // should not be persisted into the database since it's not backed by a field.
    val someOtherComputedValue: Int get() = age

    companion object : PersonDao() {
        val IGNORE_THIS_FIELD: Int = 0
        @JvmStatic
        val dao = PersonDao()

        @JdbiProperty(map = false)
        val ID = TableProperty.of<Person, Long>(Person::class.java, "id")
        @JdbiProperty(map = false)
        val NAME = TableProperty.of<Person, String>(Person::class.java, "name")
        @JdbiProperty(map = false)
        val AGE = TableProperty.of<Person, Int>(Person::class.java, "age")
        @JdbiProperty(map = false)
        val ISALIVE25 = TableProperty.of<Person, Boolean>(Person::class.java, "isAlive25")
    }

    fun withZeroNanos(): Person = copy(created = created?.withZeroMillis, modified = modified?.withZeroNanos)
}

open class PersonDao : Dao<Person, Long>(Person::class.java) {
    fun findAll2(): List<Person> = jdbi().withHandle<List<Person>, Exception> { handle ->
        handle.createQuery("select p.* from Test p")
                .map(rowMapper)
                .list()
    }
}

/**
 * A test table that tests the most basic cases. The ID is auto-generated by the database.
 */
@Table("Test")
data class Person2(
    private var id: Long? = null,
    @field:Length(min = 1)
    var name: String = "",
    var age: Int = -1,
    @field:JdbiProperty(map = false) var ignored: String? = null,
    @Transient var ignored2: Any? = null,
    var dateOfBirth: LocalDate? = null,
    // test of aliased field
    @field:ColumnName("alive")
    var isAlive25: Boolean? = null,
    var maritalStatus: MaritalStatus? = null,
    var someStringValue: String? = null

) : Entity<Long> {
    override fun getId(): Long? = id
    override fun setId(id: Long?) { this.id = id }

    companion object : Person2Dao() {
        @JvmStatic
        val dao = Person2Dao()

        @JvmStatic
        val ID = TableProperty.of<Person2, Long>(Person2::class.java, "id")
        @JvmStatic
        val NAME = TableProperty.of<Person2, String>(Person2::class.java, "name")
        @JvmStatic
        val AGE = TableProperty.of<Person2, Int>(Person2::class.java, "age")
        @JvmStatic
        val ISALIVE25 = TableProperty.of<Person2, Boolean>(Person2::class.java, "isAlive25")
        @JvmStatic
        val SOMESTRINGVALUE = TableProperty.of<Person2, String>(Person2::class.java, "someStringValue")
    }
}

open class Person2Dao : Dao<Person2, Long>(Person2::class.java)
