package com.gitlab.mvysny.jdbiorm

import com.gitlab.mvysny.jdbiorm.quirks.DatabaseVariant
import org.junit.jupiter.api.*
import org.testcontainers.DockerClientFactory
import org.testcontainers.containers.MSSQLServerContainer

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MssqlDatabaseTest {
    init {
        Assumptions.assumeTrue(DockerClientFactory.instance().isDockerAvailable(), "Docker not available")
        Assumptions.assumeTrue(isX86_64) { "MSSQL is only available on amd64: https://hub.docker.com/_/microsoft-mssql-server/ " }
    }
    private lateinit var container: MSSQLServerContainer<*>
    @BeforeAll fun setup() {
        container = MSSQLServerContainer("mcr.microsoft.com/mssql/server:${DatabaseVersions.mssql}")
        container.start()
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

    @AfterAll fun teardown() {
        JdbiOrm.destroy()
        container.stop()
    }

    @BeforeEach @AfterEach fun purgeDb() { clearDb() }

    @Test fun `expect MSSQL variant`() {
        kotlin.test.expect(DatabaseVariant.MSSQL) {
            db {
                DatabaseVariant.from(
                    this
                )
            }
        }
    }

    // unfortunately the default Docker image doesn't support the FULLTEXT index:
    // https://stackoverflow.com/questions/60489784/installing-mssql-server-express-using-docker-with-full-text-search-support
    @Nested
    inner class AllDatabaseTests : AbstractDatabaseTests(DatabaseInfo(DatabaseVariant.MSSQL, supportsFullText = false))
}
