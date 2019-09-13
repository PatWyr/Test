package com.gitlab.mvysny.jdbiorm

import com.github.mvysny.dynatest.DynaNodeGroup
import com.gitlab.mvysny.jdbiorm.JdbiOrm.jdbi
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.intellij.lang.annotations.Language
import org.jdbi.v3.core.Handle
import java.util.*

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
    JdbiOrm.setDataSource(HikariDataSource(HikariConfig().apply(block)))
}

private fun DynaNodeGroup.usingDockerizedPosgresql(databasePort: Int) {
    check(Docker.isPresent) { "Docker not available" }
    beforeGroup { Docker.startPostgresql(port = databasePort) }
    beforeGroup {
        hikari {
            minimumIdle = 0
            maximumPoolSize = 30
            // stringtype=unspecified : see https://github.com/mvysny/vok-orm/issues/12 for more details.
            jdbcUrl = "jdbc:postgresql://localhost:$databasePort/postgres?stringtype=unspecified"
            username = "postgres"
            password = "mysecretpassword"
        }
        db {
            ddl("""create table if not exists Test (
                id bigserial primary key,
                name varchar(400) not null,
                age integer not null,
                dateOfBirth date,
                created timestamp,
                modified timestamp,
                alive boolean,
                maritalStatus varchar(200)
                 )""")
            ddl("""create table if not exists EntityWithAliasedId(myid bigserial primary key, name varchar(400) not null)""")
            ddl("""create table if not exists NaturalPerson(id varchar(10) primary key, name varchar(400) not null, bytes bytea not null)""")
            ddl("""create table if not exists LogRecord(id UUID primary key, text varchar(400) not null)""")
            ddl("""CREATE TYPE marital_status AS ENUM ('Single', 'Married', 'Widowed', 'Divorced')""")
            ddl("""CREATE TABLE IF NOT EXISTS TypeMappingEntity(id bigserial primary key, enumTest marital_status)""")
            ddl("""create table JOIN_TABLE(customerId integer, orderId integer)""")
        }
    }

    afterGroup { JdbiOrm.destroy() }
    afterGroup { Docker.stopPostgresql() }

    fun clearDb() {
        Person.deleteAll()
        EntityWithAliasedId.dao.deleteAll()
        NaturalPerson.deleteAll()
        LogRecord.deleteAll()
        JoinTable.dao.deleteAll()
    }
    beforeEach { clearDb() }
    afterEach { clearDb() }
}

fun DynaNodeGroup.usingDockerizedMysql(databasePort: Int) {
    check(Docker.isPresent) { "Docker not available" }
    beforeGroup { Docker.startMysql(port = databasePort) }
    beforeGroup {
        hikari {
            minimumIdle = 0
            maximumPoolSize = 30
            jdbcUrl = "jdbc:mysql://localhost:$databasePort/db"
            username = "testuser"
            password = "mysqlpassword"
        }
        db {
            ddl("""create table if not exists Test (
                id bigint primary key auto_increment,
                name varchar(400) not null,
                age integer not null,
                dateOfBirth date,
                created timestamp(3) NULL,
                modified timestamp(3) NULL,
                alive boolean,
                maritalStatus varchar(200)
                 )""")
            ddl("""create table if not exists EntityWithAliasedId(myid bigint primary key auto_increment, name varchar(400) not null)""")
            ddl("""create table if not exists NaturalPerson(id varchar(10) primary key, name varchar(400) not null, bytes binary(16) not null)""")
            ddl("""create table if not exists LogRecord(id binary(16) primary key, text varchar(400) not null)""")
            ddl("""create table TypeMappingEntity(id bigint primary key auto_increment, enumTest ENUM('Single', 'Married', 'Divorced', 'Widowed'))""")
            ddl("""create table JOIN_TABLE(customerId bigint, orderId bigint)""")
        }
    }

    afterGroup { JdbiOrm.destroy() }
    afterGroup { Docker.stopMysql() }

    fun clearDb() {
        Person.deleteAll()
        EntityWithAliasedId.dao.deleteAll()
        NaturalPerson.deleteAll()
        LogRecord.deleteAll()
        TypeMappingEntity.deleteAll()
        JoinTable.dao.deleteAll()
    }
    beforeEach { clearDb() }
    afterEach { clearDb() }
}

fun <T> db(block: Handle.() -> T): T = jdbi().withHandle<T, Exception>(block)

fun DynaNodeGroup.usingH2Database() {
    beforeGroup {
        hikari {
            jdbcUrl = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1"
            username = "sa"
            password = ""
        }
    }

    afterGroup { JdbiOrm.destroy() }

    beforeEach {
        db {
            ddl("DROP ALL OBJECTS")
            ddl("""create table Test (
                id bigint primary key auto_increment,
                name varchar not null,
                age integer not null,
                dateOfBirth date,
                created timestamp,
                modified timestamp,
                alive boolean,
                maritalStatus varchar
                 )""")
            ddl("""create table EntityWithAliasedId(myid bigint primary key auto_increment, name varchar not null)""")
            ddl("""create table NaturalPerson(id varchar(10) primary key, name varchar(400) not null, bytes binary(16) not null)""")
            ddl("""create table LogRecord(id UUID primary key, text varchar(400) not null)""")
            ddl("""create table TypeMappingEntity(id bigint primary key auto_increment, enumTest ENUM('Single', 'Married', 'Divorced', 'Widowed'))""")
            ddl("""create table JOIN_TABLE(customerId bigint, orderId bigint)""")
        }
    }
    afterEach {
        db { ddl("DROP ALL OBJECTS") }
    }
}

fun Handle.ddl(@Language("sql") sql: String) {
    createUpdate(sql).execute()
}

private fun DynaNodeGroup.usingDockerizedMariaDB(databasePort: Int) {
    check(Docker.isPresent) { "Docker not available" }
    beforeGroup { Docker.startMariaDB(port = databasePort) }
    beforeGroup {
        hikari {
            minimumIdle = 0
            maximumPoolSize = 30
            jdbcUrl = "jdbc:mariadb://localhost:$databasePort/db"
            username = "testuser"
            password = "mysqlpassword"
        }
        db {
            ddl(
                """create table if not exists Test (
                id bigint primary key auto_increment,
                name varchar(400) not null,
                age integer not null,
                dateOfBirth date,
                created timestamp(3) NULL,
                modified timestamp(3) NULL,
                alive boolean,
                maritalStatus varchar(200)
                 )"""
            )
            ddl("""create table if not exists EntityWithAliasedId(myid bigint primary key auto_increment, name varchar(400) not null)""")
            ddl("""create table if not exists NaturalPerson(id varchar(10) primary key, name varchar(400) not null, bytes binary(16) not null)""")
            ddl("""create table if not exists LogRecord(id binary(16) primary key, text varchar(400) not null)""")
            ddl("""create table TypeMappingEntity(id bigint primary key auto_increment, enumTest ENUM('Single', 'Married', 'Divorced', 'Widowed'))""")
            ddl("""create table JOIN_TABLE(customerId bigint, orderId bigint)""")
        }
    }

    afterGroup { JdbiOrm.destroy() }
    afterGroup { Docker.stopMariaDB() }

    fun clearDb() {
        Person.deleteAll()
        EntityWithAliasedId.dao.deleteAll()
        NaturalPerson.deleteAll()
        LogRecord.deleteAll()
        JoinTable.dao.deleteAll()
    }
    beforeEach { clearDb() }
    afterEach { clearDb() }
}

fun DynaNodeGroup.withAllDatabases(block: DynaNodeGroup.()->Unit) {
    group("H2") {
        usingH2Database()
        block()
    }

    if (Docker.isPresent) {
        println("Docker is available, running tests")
        group("PostgreSQL 10.3") {
            usingDockerizedPosgresql(12345)
            block()
        }

        group("MySQL 5.7.21") {
            usingDockerizedMysql(12346)
            block()
        }

        group("MariaDB 10.1.31") {
            usingDockerizedMariaDB(12347)
            block()
        }
    } else {
        println("Docker is not available, not running PostgreSQL/MySQL/MariaDB tests")
    }
}
