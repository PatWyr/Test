package com.gitlab.mvysny.jdbiorm

import com.github.mvysny.dynatest.cloneBySerialization
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.lang.IllegalStateException
import kotlin.test.expect

abstract class AbstractJoinTableTests {
    @Nested inner class FindAllTests {
        @Test fun `no rows returned on empty table`() {
            expectList() { JoinTable.dao.findAll() }
        }
        @Test fun `all rows returned`() {
            db { (0..300).forEach { JoinTable(it, it).save() } }
            expect((0..300).toList()) {
                JoinTable.dao.findAll().map { it.customerId }.sorted()
            }
        }
        @Test fun emptyPaging() {
            db { (0..100).forEach { JoinTable(it, it).save() } }
            expectList() { JoinTable.dao.findAll(0L, 0L) }
            expectList() { JoinTable.dao.findAll(20L, 0L) }
            expectList() { JoinTable.dao.findAll(2000L, 0L) }
        }
        @Test fun paging() {
            db { (0..100).forEach { JoinTable(it, it).save() } }
            expect((0..9).toList()) {
                JoinTable.dao.findAll(
                    "customerId ASC",
                    0L,
                    10L
                ).map { it.customerId }
            }
            expect((20..49).toList()) {
                JoinTable.dao.findAll(
                    "customerId ASC",
                    20L,
                    30L
                ).map { it.customerId }
            }
            expect((90..100).toList()) {
                JoinTable.dao.findAll(
                    "customerId ASC",
                    90L,
                    300L
                ).map { it.customerId }
            }
            expectList() { JoinTable.dao.findAll(2000L, 50L) }
        }
        @Test fun sorting() {
            db { (0..100).forEach { JoinTable(it, -it).save() } }
            expect((0..9).toList()) {
                JoinTable.dao.findAll(
                    listOf(
                        JoinTable.CUSTOMERID.asc()
                    ), 0L, 10L
                ).map { it.customerId }
            }
            expect((100 downTo 91).toList()) {
                JoinTable.dao.findAll(
                    listOf(JoinTable.CUSTOMERID.desc()),
                    0L,
                    10L
                ).map { it.customerId }
            }
            expect((0..9).toList()) {
                JoinTable.dao.findAll(
                    listOf(
                        JoinTable.CUSTOMERID.asc(),
                        JoinTable.ORDERID.asc()
                    ), 0L, 10L
                ).map { it.customerId }
            }
            expect((100 downTo 91).toList()) {
                JoinTable.dao.findAll(
                    listOf(
                        JoinTable.CUSTOMERID.desc(),
                        JoinTable.ORDERID.asc()
                    ),
                    0L,
                    10L
                ).map { it.customerId }
            }
            expect((0..9).toList()) {
                JoinTable.dao.findAll(
                    listOf(
                        JoinTable.ORDERID.desc(),
                        JoinTable.CUSTOMERID.asc()
                    ), 0L, 10L
                ).map { it.customerId }
            }
            expect((100 downTo 91).toList()) {
                JoinTable.dao.findAll(
                    listOf(JoinTable.ORDERID.asc(), JoinTable.CUSTOMERID.asc()),
                    0L,
                    10L
                ).map { it.customerId }
            }
        }
        @Nested inner class FindAllByTests {
            @Test fun nonPaged() {
                val p = JoinTable(130, 10)
                p.save()
                expectList(p) {
                    JoinTable.dao.findAllBy("customerId = :cid", null, null) { it.bind("cid", 130) }
                }
                expectList(p) {
                    JoinTable.dao.findAllBy("customerId = :cid") { it.bind("cid", 130) }
                }
            }
            @Test fun paged() {
                db { (0..100).forEach { JoinTable(it, it).save() } }
                expect((20..30).toList()) {
                    JoinTable.dao.findAllBy(
                        "customerId >= :cid",
                        "customerId ASC",
                        20L,
                        11L
                    ) { it.bind("cid", 0) }
                        .map { it.customerId }
                }
            }
        }
    }
    @Nested inner class SingleByTests {
        @Test fun `succeeds if there is exactly one matching entity`() {
            val p = JoinTable(130, 10)
            p.save()
            expect(p) {
                JoinTable.dao.singleBy("customerId = :cid") {
                    it.bind(
                        "cid",
                        130
                    )
                }
            }
            expect(p) {
                JoinTable.dao.singleBy(JoinTable.CUSTOMERID.eq(130))
            }
        }

        @Test fun `fails if there is no such entity`() {
            var ex = assertThrows<IllegalStateException> {
                JoinTable.dao.singleBy("customerId = :cid") { it.bind("cid", 10) }
            }
            expect("no row matching JoinTable: 'customerId = :cid'{positional:{}, named:{cid:10}, finder:[]}") { ex.message }
            ex = assertThrows<IllegalStateException>() {
                JoinTable.dao.singleBy(JoinTable.CUSTOMERID.eq(10))
            }
            expect(true) { ex.message!!.contains("no row matching JoinTable: '(JOIN_TABLE.customerId) = (:p") }
        }

        @Test fun `fails if there are two matching entities`() {
            repeat(2) { JoinTable(100, 100).save() }
            var ex = assertThrows<IllegalStateException> {
                JoinTable.dao.singleBy("customerId = :cid") { it.bind("cid", 100) }
            }
            expect("too many rows matching JoinTable: 'customerId = :cid'{positional:{}, named:{cid:100}, finder:[]}") { ex.message }
            expectThrows<IllegalStateException>("too many rows matching JoinTable: '(JOIN_TABLE.customerId) = (:p") {
                JoinTable.dao.singleBy(JoinTable.CUSTOMERID.eq(100))
            }
        }

        @Test fun `fails if there are ten matching entities`() {
            repeat(10) { JoinTable(100, 100).save() }
            expectThrows<IllegalStateException>("too many rows matching JoinTable: 'customerId = :cid'{positional:{}, named:{cid:100}, finder:[]}") {
                JoinTable.dao.singleBy("customerId = :cid") { it.bind("cid", 100) }
            }
            expectThrows<IllegalStateException>("too many rows matching JoinTable: '(JOIN_TABLE.customerId) = (:p") {
                JoinTable.dao.singleBy(JoinTable.CUSTOMERID.eq(100))
            }
        }
    }
    @Nested inner class SingleTests {
        @Test fun `succeeds if there is exactly one matching entity`() {
            val p = JoinTable(130, 10)
            p.save()
            expect(p) {
                JoinTable.dao.single()
            }
        }

        @Test fun `fails if there is no such entity`() {
            expectThrows<IllegalStateException>("no row matching JoinTable: ''{positional:{}, named:{}, finder:[]}") {
                JoinTable.dao.single()
            }
        }

        @Test fun `fails if there are two matching entities`() {
            repeat(2) { JoinTable(100, 100).save() }
            expectThrows<IllegalStateException>("too many rows matching JoinTable: ''{positional:{}, named:{}, finder:[]}") {
                JoinTable.dao.single()
            }
        }

        @Test fun `fails if there are ten matching entities`() {
            repeat(10) { JoinTable(100, 100).save() }
            expectThrows<IllegalStateException>("too many rows matching JoinTable: ''{positional:{}, named:{}, finder:[]}") {
                JoinTable.dao.single()
            }
        }
    }
    @Nested inner class FindSingleTests() {
        @Test fun `succeeds if there is exactly one matching entity`() {
            val p = JoinTable(130, 10)
            p.save()
            expect(p) {
                JoinTable.dao.findSingle()
            }
        }

        @Test fun `returns null if there is no such entity`() {
            expect(null) { JoinTable.dao.findSingle() }
        }

        @Test fun `fails if there are two matching entities`() {
            repeat(2) { JoinTable(100, 100).save() }
            expectThrows<IllegalStateException>("too many rows matching JoinTable: ''{positional:{}, named:{}, finder:[]}") {
                JoinTable.dao.findSingle()
            }
        }

        @Test fun `fails if there are ten matching entities`() {
            repeat(10) { JoinTable(100, 100).save() }
            expectThrows<IllegalStateException>("too many rows matching JoinTable: ''{positional:{}, named:{}, finder:[]}") {
                JoinTable.dao.findSingle()
            }
        }
    }
    @Nested inner class CountTests {
        @Test fun basicCount() {
            expect(0) { JoinTable.dao.count() }
            listOf(1, 2, 3).forEach { JoinTable(it, it).save() }
            expect(3) { JoinTable.dao.count() }
        }
        @Test fun countWithFilters() {
            expect(0) { JoinTable.dao.countBy("customerId > 3") {} }
            listOf(2, 3, 4).forEach { JoinTable(it, it).save() }
            expect(1) { JoinTable.dao.countBy("customerId > 3") {} }
        }
    }
    @Test fun deleteAll() {
        listOf(1, 2, 3).forEach { JoinTable(it, it).save() }
        expect(3) { JoinTable.dao.count() }
        JoinTable.dao.deleteAll()
        expect(0) { JoinTable.dao.count() }
    }
    @Test fun deleteBy() {
        listOf(1, 2, 3).forEach { JoinTable(it, it).save() }
        JoinTable.dao.deleteBy("customerId = :cid") { q -> q.bind("cid", 2) }
        expect(listOf(1, 3)) {
            JoinTable.dao.findAll().map { it.customerId }.sorted()
        }
    }
    @Nested inner class FindSingleByTests() {
        @Test fun `succeeds if there is exactly one matching entity`() {
            val p = JoinTable(130, 130)
            p.save()
            expect(p) {
                JoinTable.dao.findSingleBy("customerId = :cid") {
                    it.bind(
                        "cid",
                        130
                    )
                }
            }
            expect(p) {
                JoinTable.dao.findSingleBy(JoinTable.CUSTOMERID.eq(130))
            }
        }

        @Test fun `returns null if there is no such entity`() {
            expect(null) {
                JoinTable.dao.findSingleBy("customerId = :cid") {
                    it.bind(
                        "cid",
                        130
                    )
                }
            }
            expect(null) {
                JoinTable.dao.findSingleBy(JoinTable.CUSTOMERID.eq(130))
            }
        }

        @Test fun `fails if there are two matching entities`() {
            repeat(2) { JoinTable(130, 130).save() }
            expectThrows<IllegalStateException>("too many rows matching JoinTable: 'customerId = :cid'{positional:{}, named:{cid:130}, finder:[]}") {
                JoinTable.dao.findSingleBy("customerId = :cid") { it.bind("cid", 130) }
            }
            expectThrows<IllegalStateException>("too many rows matching JoinTable: '(JOIN_TABLE.customerId) = (:p") {
                JoinTable.dao.findSingleBy(JoinTable.CUSTOMERID.eq(130))
            }
        }

        @Test fun `fails if there are ten matching entities`() {
            repeat(10) { JoinTable(130, 130).save() }
            expectThrows<IllegalStateException>("too many rows matching JoinTable: 'customerId = :cid'{positional:{}, named:{cid:130}, finder:[]}") {
                JoinTable.dao.findSingleBy("customerId = :cid") { it.bind("cid", 130) }
            }
            expectThrows<IllegalStateException>("too many rows matching JoinTable: '(JOIN_TABLE.customerId) = (:p") {
                JoinTable.dao.findSingleBy(JoinTable.CUSTOMERID.eq(130))
            }
        }
    }
    @Nested inner class FindFirstTests {
        @Test fun `succeeds if there is exactly one entity`() {
            val p = JoinTable(130, 130)
            p.save()
            expect(p) {
                JoinTable.dao.findFirst()
            }
        }

        @Test fun `returns null if there is no such entity`() {
            expect(null) {
                JoinTable.dao.findFirst()
            }
        }

        @Test fun `returns random if there are two matching entities`() {
            repeat(2) { JoinTable(130, 130).save() }
            expect(JoinTable(130, 130)) {
                JoinTable.dao.findFirst()
            }
        }
    }
    @Nested inner class FindFirstByTests() {
        @Test fun `succeeds if there is exactly one matching entity`() {
            val p = JoinTable(130, 130)
            p.save()
            expect(p) {
                JoinTable.dao.findFirstBy("customerId = :cid") {
                    it.bind(
                        "cid",
                        130
                    )
                }
            }
            expect(p) {
                JoinTable.dao.findFirstBy(JoinTable.CUSTOMERID.eq(130))
            }
        }

        @Test fun `returns null if there is no such entity`() {
            expect(null) {
                JoinTable.dao.findFirstBy("customerId = :cid") {
                    it.bind(
                        "cid",
                        130
                    )
                }
            }
            expect(null) {
                JoinTable.dao.findFirstBy(JoinTable.CUSTOMERID.eq(130))
            }
        }

        @Test fun `returns random if there are two matching entities`() {
            repeat(2) { JoinTable(130, 130).save() }
            expect(JoinTable(130, 130)) {
                JoinTable.dao.findFirstBy("customerId = :cid") {
                    it.bind(
                        "cid",
                        130
                    )
                }
            }
            expect(JoinTable(130, 130)) {
                JoinTable.dao.findFirstBy(JoinTable.CUSTOMERID.eq(130))
            }
        }
    }
    @Nested inner class ExistsTests {
        @Test fun `returns false on empty table`() {
            expect(false) { JoinTable.dao.existsAny() }
            expect(false) { JoinTable.dao.existsBy("customerId=0") {} }
        }
        @Test fun `returns true on matching entity`() {
            val p = JoinTable(100, 100)
            p.save()
            expect(true) { JoinTable.dao.existsAny() }
            expect(true) { JoinTable.dao.existsBy("customerId>=50") {} }
        }
        @Test fun `returns false on non-matching entity`() {
            val p = JoinTable(100, 100)
            p.save()
            expect(false) { JoinTable.dao.existsBy("customerId>=200") {} }
        }
    }
    @Test fun serializable() {
        DaoOfAny(JoinTable::class.java).cloneBySerialization()
    }
}