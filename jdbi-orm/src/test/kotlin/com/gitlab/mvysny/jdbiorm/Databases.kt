package com.gitlab.mvysny.jdbiorm

import com.gitlab.mvysny.jdbiorm.quirks.DatabaseVariant
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.util.*

object DatabaseVersions {
    val postgres = "16.3" // https://hub.docker.com/_/postgres/
    val cockroach = "v23.2.6" // https://hub.docker.com/r/cockroachdb/cockroach
    val mysql = "9.0.0" // https://hub.docker.com/_/mysql/
    val mssql = "2017-latest-ubuntu"
    val mariadb = "11.2.4" // https://hub.docker.com/_/mariadb
}

enum class MaritalStatus {
    Single,
    Married,
    Divorced,
    Widowed
}

/**
 * A table demoing natural person with government-issued ID (birth number, social security number, etc).
 */
data class NaturalPerson(private var id: String? = null, var name: String = "", var bytes: ByteArray = byteArrayOf()) : Entity<String> {
    override fun getId(): String? = id
    override fun setId(id: String?) { this.id = id }
    companion object : Dao<NaturalPerson, String>(NaturalPerson::class.java)
}

/**
 * Demoes app-generated UUID ids. Note how [create] is overridden to auto-generate the ID, so that [save] works properly.
 *
 * Warning: do NOT add any additional fields in here, since that would make java place synthetic `setId(Object) before
 * `setId(UUID)` and we wouldn't test the metadata hook that fixes this issue.
 *
 * [id] is mapped to UUID in MySQL which is binary(16)
 */
data class LogRecord(private var id: UUID? = null, var text: String = "") : UuidEntity {
    override fun getId(): UUID? = id
    override fun setId(id: UUID?) { this.id = id }
    companion object : Dao<LogRecord, UUID>(LogRecord::class.java)
}

/**
 * Tests all sorts of type mapping:
 * @property enumTest tests Java Enum mapping to native database enum mapping: https://github.com/mvysny/vok-orm/issues/12
 */
data class TypeMappingEntity(private var id: Long? = null,
                             var enumTest: MaritalStatus? = null
                             ) : Entity<Long> {
    override fun getId(): Long? = id
    override fun setId(id: Long?) { this.id = id }
    companion object : Dao<TypeMappingEntity, Long>(TypeMappingEntity::class.java)
}

fun hikari(block: HikariConfig.() -> Unit) {
    JdbiOrm.databaseVariant = null
    JdbiOrm.setDataSource(HikariDataSource(HikariConfig().apply(block)))
}

data class DatabaseInfo(val variant: DatabaseVariant, val supportsFullText: Boolean = true)
