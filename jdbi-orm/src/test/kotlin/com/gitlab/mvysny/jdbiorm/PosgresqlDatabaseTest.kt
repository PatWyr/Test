package com.gitlab.mvysny.jdbiorm

import com.gitlab.mvysny.jdbiorm.quirks.DatabaseVariant
import org.junit.jupiter.api.*
import org.testcontainers.DockerClientFactory
import org.testcontainers.containers.PostgreSQLContainer
import kotlin.test.expect

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PosgresqlDatabaseTest {
    init {
        Assumptions.assumeTrue(DockerClientFactory.instance().isDockerAvailable(), "Docker not available")
    }
    private lateinit var container: PostgreSQLContainer<*>
    @BeforeAll fun setup() {
        container = PostgreSQLContainer("postgres:${DatabaseVersions.postgres}") // https://hub.docker.com/_/postgres/
        container.start()

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

    @AfterAll fun teardown() {
        JdbiOrm.destroy()
        if (::container.isInitialized) {
            container.stop()
        }
    }

    @BeforeEach @AfterEach fun purgeDb() { clearDb() }

    @Test fun `expect PostgreSQL variant`() {
        expect(DatabaseVariant.PostgreSQL) {
            db {
                DatabaseVariant.from(this)
            }
        }
    }

    @Nested
    inner class AllDatabaseTests : AbstractDatabaseTests(DatabaseInfo(DatabaseVariant.PostgreSQL))
}
