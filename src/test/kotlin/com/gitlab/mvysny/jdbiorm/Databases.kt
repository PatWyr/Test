package com.gitlab.mvysny.jdbiorm

import com.github.mvysny.dynatest.DynaNodeGroup
import com.gitlab.mvysny.jdbiorm.JdbiOrm.jdbi
import com.gitlab.mvysny.jdbiorm.quirks.DatabaseVariant
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.intellij.lang.annotations.Language
import org.jdbi.v3.core.Handle
import org.testcontainers.DockerClientFactory
import org.testcontainers.containers.MSSQLServerContainer
import org.testcontainers.containers.MariaDBContainer
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.containers.PostgreSQLContainer
import java.util.*
import kotlin.test.expect

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

private fun DynaNodeGroup.usingDockerizedPosgresql() {
    check(DockerClientFactory.instance().isDockerAvailable()) { "Docker not available" }
    lateinit var container: PostgreSQLContainer<*>
    beforeGroup {
        container = PostgreSQLContainer("postgres:10.3")
        container.start()
    }
    beforeGroup {
        hikari {
            minimumIdle = 0
            maximumPoolSize = 30
            // stringtype=unspecified : see https://github.com/mvysny/vok-orm/issues/12 for more details.
            jdbcUrl = container.jdbcUrl.removeSuffix("loggerLevel=OFF") + "stringtype=unspecified"
            username = "test"
            password = "test"
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
            ddl("""create table mapping_table(person_id bigint not null, department_id bigint not null, some_data varchar(400) not null, PRIMARY KEY(person_id, department_id))""")
        }
    }

    afterGroup { JdbiOrm.destroy() }
    afterGroup { container.stop() }

    beforeEach { clearDb() }
    afterEach { clearDb() }

    test("expect PostgreSQL variant") {
        expect(DatabaseVariant.PostgreSQL) { db { DatabaseVariant.from(this) } }
    }
}

fun DynaNodeGroup.usingDockerizedMysql() {
    check(DockerClientFactory.instance().isDockerAvailable()) { "Docker not available" }
    lateinit var container: MySQLContainer<*>
    beforeGroup {
        container = MySQLContainer("mysql:5.7.21")
        // disable SSL, to avoid SSL-related exceptions on github actions:
        // javax.net.ssl.SSLHandshakeException: No appropriate protocol (protocol is disabled or cipher suites are inappropriate)
        container.withUrlParam("useSSL", "false")
        container.start()
    }
    beforeGroup {
        hikari {
            minimumIdle = 0
            maximumPoolSize = 30
            jdbcUrl = container.jdbcUrl
            username = "test"
            password = "test"
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
            ddl("""create table EntityWithAliasedId(myid bigint primary key auto_increment, name varchar(400) not null)""")
            ddl("""create table NaturalPerson(id varchar(10) primary key, name varchar(400) not null, bytes binary(16) not null)""")
            ddl("""create table LogRecord(id binary(16) primary key, text varchar(400) not null)""")
            ddl("""create table TypeMappingEntity(id bigint primary key auto_increment, enumTest ENUM('Single', 'Married', 'Divorced', 'Widowed'))""")
            ddl("""create table JOIN_TABLE(customerId bigint, orderId bigint)""")
            ddl("""create table mapping_table(person_id bigint not null, department_id bigint not null, some_data varchar(400) not null, PRIMARY KEY(person_id, department_id))""")
        }
    }

    afterGroup { JdbiOrm.destroy() }
    afterGroup { container.stop() }

    beforeEach { clearDb() }
    afterEach { clearDb() }

    test("expect MySQL variant") {
        expect(DatabaseVariant.MySQLMariaDB) { db { DatabaseVariant.from(this) } }
    }
}

private fun clearDb() {
    Person.deleteAll()
    EntityWithAliasedId.dao.deleteAll()
    NaturalPerson.deleteAll()
    LogRecord.deleteAll()
    TypeMappingEntity.deleteAll()
    JoinTable.dao.deleteAll()
    MappingTable.dao.deleteAll()
}

fun <T> db(block: Handle.() -> T): T = jdbi().inTransaction<T, Exception>(block)

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
            ddl("""create table mapping_table(person_id bigint not null, department_id bigint not null, some_data varchar(400) not null, PRIMARY KEY(person_id, department_id))""")
        }
    }
    afterEach {
        db { ddl("DROP ALL OBJECTS") }
    }

    test("expect H2 variant") {
        expect(DatabaseVariant.H2) { db { DatabaseVariant.from(this) } }
    }
}

fun Handle.ddl(@Language("sql") sql: String) {
    createUpdate(sql).execute()
}

private fun DynaNodeGroup.usingDockerizedMariaDB() {
    check(DockerClientFactory.instance().isDockerAvailable()) { "Docker not available" }
    lateinit var container: MariaDBContainer<*>
    beforeGroup {
        container = MariaDBContainer("mariadb:10.1.31")
        container.start()
    }
    beforeGroup {
        hikari {
            minimumIdle = 0
            maximumPoolSize = 30
            jdbcUrl = container.jdbcUrl
            username = "test"
            password = "test"
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
            ddl("""create table mapping_table(person_id bigint not null, department_id bigint not null, some_data varchar(400) not null, PRIMARY KEY(person_id, department_id))""")
        }
    }

    afterGroup { JdbiOrm.destroy() }
    afterGroup { container.stop() }

    beforeEach { clearDb() }
    afterEach { clearDb() }

    test("expect MySQL variant") {
        expect(DatabaseVariant.MySQLMariaDB) { db { DatabaseVariant.from(this) } }
    }
}

private fun DynaNodeGroup.usingDockerizedMSSQL() {
    check(DockerClientFactory.instance().isDockerAvailable()) { "Docker not available" }
    lateinit var container: MSSQLServerContainer<*>
    beforeGroup {
        container = MSSQLServerContainer("mcr.microsoft.com/mssql/server:2017-latest-ubuntu")
        container.start()
    }
    beforeGroup {
        hikari {
            minimumIdle = 0
            maximumPoolSize = 30
            jdbcUrl = container.jdbcUrl
            username = container.username
            password = container.password
        }
        db {
            ddl(
                    """create table Test (
                id bigint primary key IDENTITY(1,1) not null,
                name varchar(400) not null,
                age integer not null,
                dateOfBirth datetime NULL,
                created datetime NULL,
                modified datetime NULL,
                alive bit,
                maritalStatus varchar(200)
                 )"""
            )
            ddl("""create table EntityWithAliasedId(myid bigint primary key IDENTITY(1,1) not null, name varchar(400) not null)""")
            ddl("""create table NaturalPerson(id varchar(10) primary key, name varchar(400) not null, bytes binary(16) not null)""")
            ddl("""create table LogRecord(id uniqueidentifier primary key, text varchar(400) not null)""")
            ddl("""create table TypeMappingEntity(id bigint primary key IDENTITY(1,1) not null, enumTest varchar(10))""")
            ddl("""create table JOIN_TABLE(customerId bigint, orderId bigint)""")
            ddl("""create table mapping_table(person_id bigint not null, department_id bigint not null, some_data varchar(400) not null, PRIMARY KEY(person_id, department_id))""")
        }
    }

    afterGroup { JdbiOrm.destroy() }
    afterGroup { container.stop() }

    beforeEach { clearDb() }
    afterEach { clearDb() }

    test("expect MSSQL variant") {
        expect(DatabaseVariant.MSSQL) { db { DatabaseVariant.from(this) } }
    }
}

fun DynaNodeGroup.withAllDatabases(block: DynaNodeGroup.()->Unit) {
    group("H2") {
        usingH2Database()
        block()
    }

    if (System.getProperty("h2only").toBoolean()) {
        println("`h2only` system property specified, skipping PostgreSQL/MySQL/MariaDB/MSSQL tests")
    } else if (!DockerClientFactory.instance().isDockerAvailable) {
        println("Docker is not available, not running PostgreSQL/MySQL/MariaDB/MSSQL tests")
    } else {
        println("Docker is available, running PostgreSQL/MySQL/MariaDB tests")
        group("PostgreSQL 10.3") {
            usingDockerizedPosgresql()
            block()
        }

        group("MySQL 5.7.21") {
            usingDockerizedMysql()
            block()
        }

        group("MariaDB 10.1.31") {
            usingDockerizedMariaDB()
            block()
        }

        group("MSSQL 2017 Express") {
            usingDockerizedMSSQL()
            block()
        }
    }
}
