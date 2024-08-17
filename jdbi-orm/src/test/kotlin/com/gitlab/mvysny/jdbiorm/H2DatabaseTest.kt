package com.gitlab.mvysny.jdbiorm

import com.gitlab.mvysny.jdbiorm.quirks.DatabaseVariant
import org.junit.jupiter.api.*

/**
 * Tests JDBI-ORM on H2.
 */
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
    fun destroyJdbi() {
        JdbiOrm.destroy()
    }

    @BeforeEach
    fun setupDatabase() {
        db {
            ddl("DROP ALL OBJECTS")
            ddl("""CREATE ALIAS IF NOT EXISTS FTL_INIT FOR "org.h2.fulltext.FullTextLucene.init";CALL FTL_INIT();""")
            ddl(
                """create table Test (
                id bigint primary key auto_increment,
                name varchar not null,
                age integer not null,
                dateOfBirth date,
                created timestamp,
                modified timestamp,
                alive boolean,
                maritalStatus varchar,
                someStringValue varchar
                 )"""
            )
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
        kotlin.test.expect(DatabaseVariant.H2) { db { DatabaseVariant.from(this) } }
    }

    @Nested
    inner class AllDatabaseTests : AbstractDatabaseTests(
        DatabaseInfo(
            DatabaseVariant.H2
        )
    )
}