package com.gitlab.mvysny.jdbiorm

import com.gitlab.mvysny.jdbiorm.quirks.DatabaseVariant
import org.junit.jupiter.api.*
import org.testcontainers.DockerClientFactory
import org.testcontainers.containers.MySQLContainer
import kotlin.test.expect

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MysqlDatabaseTest {
    init {
        Assumptions.assumeTrue(DockerClientFactory.instance().isDockerAvailable(), "Docker not available")
    }
    private lateinit var container: MySQLContainer<*>

    @BeforeAll fun runMysqlContainer() {
        container = MySQLContainer("mysql:${DatabaseVersions.mysql}")
        // disable SSL, to avoid SSL-related exceptions on github actions:
        // javax.net.ssl.SSLHandshakeException: No appropriate protocol (protocol is disabled or cipher suites are inappropriate)
        container.withUrlParam("useSSL", "false")
        container.start()

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

    @AfterAll
    fun tearDownMysql() {
        JdbiOrm.destroy()
        if (::container.isInitialized) {
            container.stop()
        }
    }

    @BeforeEach @AfterEach fun purgeDb() { clearDb() }

    @Test fun `expect MySQL variant`() {
        expect(DatabaseVariant.MySQLMariaDB) {
            db {
                DatabaseVariant.from(this)
            }
        }
    }

    @Nested
    inner class AllDatabaseTests : AbstractDatabaseTests(DatabaseInfo(DatabaseVariant.MySQLMariaDB))
}
