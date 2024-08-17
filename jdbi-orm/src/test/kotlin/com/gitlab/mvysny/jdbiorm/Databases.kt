package com.gitlab.mvysny.jdbiorm

import com.github.mvysny.dynatest.DynaNodeGroup
import com.github.mvysny.dynatest.DynaTestDsl
import com.gitlab.mvysny.jdbiorm.JdbiOrm.jdbi
import com.gitlab.mvysny.jdbiorm.quirks.DatabaseVariant
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.intellij.lang.annotations.Language
import org.jdbi.v3.core.Handle
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.DockerClientFactory
import org.testcontainers.containers.CockroachContainer
import org.testcontainers.containers.MSSQLServerContainer
import org.testcontainers.containers.MariaDBContainer
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.containers.PostgreSQLContainer
import java.util.*
import kotlin.test.expect

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

abstract class AbstractDockerizedPostgreSQL

@DynaTestDsl
private fun DynaNodeGroup.usingDockerizedPosgresql() {
    check(DockerClientFactory.instance().isDockerAvailable()) { "Docker not available" }
    lateinit var container: PostgreSQLContainer<*>
    beforeGroup {
        container = PostgreSQLContainer("postgres:${DatabaseVersions.postgres}") // https://hub.docker.com/_/postgres/
        container.start()
    }
    beforeGroup {
        hikari {
            minimumIdle = 0
            maximumPoolSize = 30
            // stringtype=unspecified : see https://github.com/mvysny/vok-orm/issues/12 for more details.
            jdbcUrl = container.jdbcUrl.removeSuffix("loggerLevel=OFF") + "stringtype=unspecified"
            username = container.username
            password = container.password
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
                maritalStatus varchar(200),
                someStringValue varchar(200)
                 )""")
            ddl("""CREATE INDEX pgweb_idx ON Test USING GIN (to_tsvector('english', name));""")
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

@DynaTestDsl
private fun DynaNodeGroup.usingDockerizedCockroachDB() {
    check(DockerClientFactory.instance().isDockerAvailable()) { "Docker not available" }
    lateinit var container: CockroachContainer
    beforeGroup {
        container = CockroachContainer("cockroachdb/cockroach:${DatabaseVersions.cockroach}")
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
            ddl("""create table if not exists Test (
                id bigserial primary key,
                name varchar(400) not null,
                age integer not null,
                dateOfBirth date,
                created timestamp,
                modified timestamp,
                alive boolean,
                maritalStatus varchar(200),
                someStringValue varchar(200)
                 )""")
            // full-text search not yet supported: https://github.com/cockroachdb/cockroach/issues/41288
//            ddl("""CREATE INDEX pgweb_idx ON Test USING GIN (to_tsvector('english', name));""")
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

@DynaTestDsl
fun DynaNodeGroup.usingDockerizedMysql() {
    check(DockerClientFactory.instance().isDockerAvailable()) { "Docker not available" }
    lateinit var container: MySQLContainer<*>
    beforeGroup {
        container = MySQLContainer("mysql:${DatabaseVersions.mysql}")
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
            username = container.username
            password = container.password
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
                maritalStatus varchar(200),
                someStringValue varchar(200),
                FULLTEXT index (name)
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

@DynaTestDsl
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
            ddl("""CREATE ALIAS IF NOT EXISTS FTL_INIT FOR "org.h2.fulltext.FullTextLucene.init";CALL FTL_INIT();""")
            ddl("""create table Test (
                id bigint primary key auto_increment,
                name varchar not null,
                age integer not null,
                dateOfBirth date,
                created timestamp,
                modified timestamp,
                alive boolean,
                maritalStatus varchar,
                someStringValue varchar
                 )""")
            ddl("""create table EntityWithAliasedId(myid bigint primary key auto_increment, name varchar not null)""")
            ddl("""create table NaturalPerson(id varchar(10) primary key, name varchar(400) not null, bytes binary(16) not null)""")
            ddl("""create table LogRecord(id UUID primary key, text varchar(400) not null)""")
            ddl("""create table TypeMappingEntity(id bigint primary key auto_increment, enumTest ENUM('Single', 'Married', 'Divorced', 'Widowed'))""")
            ddl("""create table JOIN_TABLE(customerId bigint, orderId bigint)""")
            ddl("""create table mapping_table(person_id bigint not null, department_id bigint not null, some_data varchar(400) not null, PRIMARY KEY(person_id, department_id))""")
            ddl("""CALL FTL_CREATE_INDEX('PUBLIC', 'TEST', 'NAME');""")
        }
    }
    afterEach {
        db { ddl("DROP ALL OBJECTS") }
    }

    test("expect H2 variant") {
        expect(DatabaseVariant.H2) { db { DatabaseVariant.from(this) } }
    }
}

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class H2DatabaseTest {
    @BeforeAll
    fun setupJdbi() {
        hikari {
            jdbcUrl = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1"
            username = "sa"
            password = ""
        }
    }

    @AfterAll
    fun destroyJdbi() { JdbiOrm.destroy() }

    @BeforeEach
    fun setupDatabase() {
        db {
            ddl("DROP ALL OBJECTS")
            ddl("""CREATE ALIAS IF NOT EXISTS FTL_INIT FOR "org.h2.fulltext.FullTextLucene.init";CALL FTL_INIT();""")
            ddl("""create table Test (
                id bigint primary key auto_increment,
                name varchar not null,
                age integer not null,
                dateOfBirth date,
                created timestamp,
                modified timestamp,
                alive boolean,
                maritalStatus varchar,
                someStringValue varchar
                 )""")
            ddl("""create table EntityWithAliasedId(myid bigint primary key auto_increment, name varchar not null)""")
            ddl("""create table NaturalPerson(id varchar(10) primary key, name varchar(400) not null, bytes binary(16) not null)""")
            ddl("""create table LogRecord(id UUID primary key, text varchar(400) not null)""")
            ddl("""create table TypeMappingEntity(id bigint primary key auto_increment, enumTest ENUM('Single', 'Married', 'Divorced', 'Widowed'))""")
            ddl("""create table JOIN_TABLE(customerId bigint, orderId bigint)""")
            ddl("""create table mapping_table(person_id bigint not null, department_id bigint not null, some_data varchar(400) not null, PRIMARY KEY(person_id, department_id))""")
            ddl("""CALL FTL_CREATE_INDEX('PUBLIC', 'TEST', 'NAME');""")
        }
    }
    @AfterEach
    fun dropDatabase() {
        db { ddl("DROP ALL OBJECTS") }
    }

    @Test
    fun expectH2Variant() {
        expect(DatabaseVariant.H2) { db { DatabaseVariant.from(this) } }
    }

    @Nested
    inner class JdbiTests : AbstractJdbiTests()
}

fun Handle.ddl(@Language("sql") sql: String) {
    execute(sql)
}

@DynaTestDsl
private fun DynaNodeGroup.usingDockerizedMariaDB() {
    check(DockerClientFactory.instance().isDockerAvailable()) { "Docker not available" }
    lateinit var container: MariaDBContainer<*>
    beforeGroup {
        container = MariaDBContainer("mariadb:${DatabaseVersions.mariadb}")
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
                """create table if not exists Test (
                id bigint primary key auto_increment,
                name varchar(400) not null,
                age integer not null,
                dateOfBirth date,
                created timestamp(3) NULL,
                modified timestamp(3) NULL,
                alive boolean,
                maritalStatus varchar(200),
                someStringValue varchar(200),
                FULLTEXT index (name)
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

private val isX86_64: Boolean get() = System.getProperty("os.arch") == "amd64"

@DynaTestDsl
private fun DynaNodeGroup.usingDockerizedMSSQL() {
    check(DockerClientFactory.instance().isDockerAvailable()) { "Docker not available" }
    check(isX86_64) { "MSSQL is only available on amd64: https://hub.docker.com/_/microsoft-mssql-server/ "}
    lateinit var container: MSSQLServerContainer<*>
    beforeGroup {
        container = MSSQLServerContainer("mcr.microsoft.com/mssql/server:${DatabaseVersions.mssql}")
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
                maritalStatus varchar(200),
                someStringValue varchar(200)
                 )"""
            )
            // unfortunately the default Docker image doesn't support the FULLTEXT index:
            // https://stackoverflow.com/questions/60489784/installing-mssql-server-express-using-docker-with-full-text-search-support
            // just skip the tests for now
            /*
                        ddl("CREATE UNIQUE INDEX ui_ukDoc ON Test(name);")
                        ddl("""CREATE FULLTEXT INDEX ON Test
            (
                Test                         --Full-text index column name
                    TYPE COLUMN name    --Name of column that contains file type information
                    Language 2057                 --2057 is the LCID for British English
            )
            KEY INDEX ui_ukDoc ON AdvWksDocFTCat --Unique index
            WITH CHANGE_TRACKING AUTO            --Population type;  """)
            */

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

@DynaTestDsl
fun DynaNodeGroup.withAllDatabases(block: DynaNodeGroup.(DatabaseInfo)->Unit) {
    group("H2") {
        usingH2Database()
        block(DatabaseInfo(DatabaseVariant.H2))
    }

    if (System.getProperty("h2only").toBoolean()) {
        println("`h2only` system property specified, skipping PostgreSQL/MySQL/MariaDB/MSSQL tests")
    } else if (!DockerClientFactory.instance().isDockerAvailable) {
        println("Docker is not available, not running PostgreSQL/MySQL/MariaDB/MSSQL tests")
    } else {
        println("Docker is available, running PostgreSQL/MySQL/MariaDB tests")
        group("PostgreSQL ${DatabaseVersions.postgres}") {
            usingDockerizedPosgresql()
            block(DatabaseInfo(DatabaseVariant.PostgreSQL))
        }

        group("MySQL ${DatabaseVersions.mysql}") {
            usingDockerizedMysql()
            block(DatabaseInfo(DatabaseVariant.MySQLMariaDB))
        }

        group("MariaDB ${DatabaseVersions.mariadb}") {
            usingDockerizedMariaDB()
            block(DatabaseInfo(DatabaseVariant.MySQLMariaDB))
        }

        if (isX86_64) {
            group("MSSQL ${DatabaseVersions.mssql}") {
                usingDockerizedMSSQL()
                // unfortunately the default Docker image doesn't support the FULLTEXT index:
                // https://stackoverflow.com/questions/60489784/installing-mssql-server-express-using-docker-with-full-text-search-support
                block(DatabaseInfo(DatabaseVariant.MSSQL, supportsFullText = false))
            }
        }

        group("CockroachDB ${DatabaseVersions.cockroach}") {
            usingDockerizedCockroachDB()
            // full-text search not yet supported: https://github.com/cockroachdb/cockroach/issues/41288
            block(DatabaseInfo(DatabaseVariant.PostgreSQL, supportsFullText = false))
        }
    }
}

data class DatabaseInfo(val variant: DatabaseVariant, val supportsFullText: Boolean = true)
