package com.gitlab.mvysny.jdbiorm

import com.github.mvysny.dynatest.*
import java.lang.IllegalStateException
import kotlin.reflect.KProperty
import kotlin.test.expect

@DynaTestDsl
fun DynaNodeGroup.joinTableTestSuite() {
    group("findAll") {
        test("no rows returned on empty table") {
            expectList() { JoinTable.dao.findAll() }
        }
        test("all rows returned") {
            db { (0..300).forEach { JoinTable(it, it).save() } }
            expect((0..300).toList()) { JoinTable.dao.findAll().map { it.customerId } .sorted() }
        }
        test("empty paging") {
            db { (0..100).forEach { JoinTable(it, it).save() } }
            expectList() { JoinTable.dao.findAll(0L, 0L) }
            expectList() { JoinTable.dao.findAll(20L, 0L) }
            expectList() { JoinTable.dao.findAll(2000L, 0L) }
        }
        test("paging") {
            db { (0..100).forEach { JoinTable(it, it).save() } }
            expect((0..9).toList()) { JoinTable.dao.findAll("customerId ASC", 0L, 10L).map { it.customerId } }
            expect((20..49).toList()) { JoinTable.dao.findAll("customerId ASC", 20L, 30L).map { it.customerId } }
            expect((90..100).toList()) { JoinTable.dao.findAll("customerId ASC", 90L, 300L).map { it.customerId } }
            expectList() { JoinTable.dao.findAll(2000L, 50L) }
        }
        test("sorting") {
            db { (0..100).forEach { JoinTable(it, -it).save() } }
            expect((0..9).toList()) { JoinTable.dao.findAll(listOf(JoinTable.CUSTOMERID.asc()), 0L, 10L).map { it.customerId } }
            expect((100 downTo 91).toList()) { JoinTable.dao.findAll(listOf(JoinTable.CUSTOMERID.desc()), 0L, 10L).map { it.customerId } }
            expect((0..9).toList()) { JoinTable.dao.findAll(listOf(JoinTable.CUSTOMERID.asc(), JoinTable.ORDERID.asc()), 0L, 10L).map { it.customerId } }
            expect((100 downTo 91).toList()) { JoinTable.dao.findAll(listOf(JoinTable.CUSTOMERID.desc(), JoinTable.ORDERID.asc()), 0L, 10L).map { it.customerId } }
            expect((0..9).toList()) { JoinTable.dao.findAll(listOf(JoinTable.ORDERID.desc(), JoinTable.CUSTOMERID.asc()), 0L, 10L).map { it.customerId } }
            expect((100 downTo 91).toList()) { JoinTable.dao.findAll(listOf(JoinTable.ORDERID.asc(), JoinTable.CUSTOMERID.asc()), 0L, 10L).map { it.customerId } }
        }
        group("findAllBy") {
            test("non-paged") {
                val p = JoinTable(130, 10)
                p.save()
                expectList(p) {
                    JoinTable.dao.findAllBy("customerId = :cid", null, null) { it.bind("cid", 130) }
                }
                expectList(p) {
                    JoinTable.dao.findAllBy("customerId = :cid") { it.bind("cid", 130) }
                }
            }
            test("paged") {
                db { (0..100).forEach { JoinTable(it, it).save() } }
                expect((20..30).toList()) {
                    JoinTable.dao.findAllBy("customerId >= :cid", "customerId ASC",20L, 11L) { it.bind("cid", 0) }
                            .map { it.customerId }
                }
            }
        }
    }
    group("singleBy() tests") {
        test("succeeds if there is exactly one matching entity") {
            val p = JoinTable(130, 10)
            p.save()
            expect(p) {
                JoinTable.dao.singleBy("customerId = :cid") { it.bind("cid", 130) }
            }
        }

        test("fails if there is no such entity") {
            expectThrows<IllegalStateException>("no row matching JoinTable: 'customerId = :cid'{positional:{}, named:{cid:10}, finder:[]}") {
                JoinTable.dao.singleBy("customerId = :cid") { it.bind("cid", 10) }
            }
        }

        test("fails if there are two matching entities") {
            repeat(2) { JoinTable(100, 100).save() }
            expectThrows<IllegalStateException>("too many rows matching JoinTable: 'customerId = :cid'{positional:{}, named:{cid:100}, finder:[]}") {
                JoinTable.dao.singleBy("customerId = :cid") { it.bind("cid", 100) }
            }
        }

        test("fails if there are ten matching entities") {
            repeat(10) { JoinTable(100, 100).save() }
            expectThrows<IllegalStateException>("too many rows matching JoinTable: 'customerId = :cid'{positional:{}, named:{cid:100}, finder:[]}") {
                JoinTable.dao.singleBy("customerId = :cid") { it.bind("cid", 100) }
            }
        }
    }
    group("single() tests") {
        test("succeeds if there is exactly one matching entity") {
            val p = JoinTable(130, 10)
            p.save()
            expect(p) {
                JoinTable.dao.single()
            }
        }

        test("fails if there is no such entity") {
            expectThrows<IllegalStateException>("no row matching JoinTable: ''{positional:{}, named:{}, finder:[]}") {
                JoinTable.dao.single()
            }
        }

        test("fails if there are two matching entities") {
            repeat(2) { JoinTable(100, 100).save() }
            expectThrows<IllegalStateException>("too many rows matching JoinTable: ''{positional:{}, named:{}, finder:[]}") {
                JoinTable.dao.single()
            }
        }

        test("fails if there are ten matching entities") {
            repeat(10) { JoinTable(100, 100).save() }
            expectThrows<IllegalStateException>("too many rows matching JoinTable: ''{positional:{}, named:{}, finder:[]}") {
                JoinTable.dao.single()
            }
        }
    }
    group("findSingle() tests") {
        test("succeeds if there is exactly one matching entity") {
            val p = JoinTable(130, 10)
            p.save()
            expect(p) {
                JoinTable.dao.findSingle()
            }
        }

        test("returns null if there is no such entity") {
            expect(null) { JoinTable.dao.findSingle() }
        }

        test("fails if there are two matching entities") {
            repeat(2) { JoinTable(100, 100).save() }
            expectThrows<IllegalStateException>("too many rows matching JoinTable: ''{positional:{}, named:{}, finder:[]}") {
                JoinTable.dao.findSingle()
            }
        }

        test("fails if there are ten matching entities") {
            repeat(10) { JoinTable(100, 100).save() }
            expectThrows<IllegalStateException>("too many rows matching JoinTable: ''{positional:{}, named:{}, finder:[]}") {
                JoinTable.dao.findSingle()
            }
        }
    }
    group("count") {
        test("basic count") {
            expect(0) { JoinTable.dao.count() }
            listOf(1, 2, 3).forEach { JoinTable(it, it).save() }
            expect(3) { JoinTable.dao.count() }
        }
        test("count with filters") {
            expect(0) { JoinTable.dao.countBy("customerId > 3") {} }
            listOf(2, 3, 4).forEach { JoinTable(it, it).save() }
            expect(1) { JoinTable.dao.countBy("customerId > 3") {} }
        }
    }
    test("DeleteAll") {
        listOf(1, 2, 3).forEach { JoinTable(it, it).save() }
        expect(3) { JoinTable.dao.count() }
        JoinTable.dao.deleteAll()
        expect(0) { JoinTable.dao.count() }
    }
    test("DeleteBy") {
        listOf(1, 2, 3).forEach { JoinTable(it, it).save() }
        JoinTable.dao.deleteBy("customerId = :cid") { q -> q.bind("cid", 2) }
        expect(listOf(1, 3)) { JoinTable.dao.findAll().map { it.customerId } .sorted() }
    }
    group("findSingleBy() tests") {
        test("succeeds if there is exactly one matching entity") {
            val p = JoinTable(130, 130)
            p.save()
            expect(p) {
                JoinTable.dao.findSingleBy("customerId = :cid") { it.bind("cid", 130) }
            }
        }

        test("returns null if there is no such entity") {
            expect(null) {
                JoinTable.dao.findSingleBy("customerId = :cid") { it.bind("cid", 130) }
            }
        }

        test("fails if there are two matching entities") {
            repeat(2) { JoinTable(130, 130).save() }
            expectThrows(IllegalStateException::class, "too many rows matching JoinTable: 'customerId = :cid'{positional:{}, named:{cid:130}, finder:[]}") {
                JoinTable.dao.findSingleBy("customerId = :cid") { it.bind("cid", 130) }
            }
        }

        test("fails if there are ten matching entities") {
            repeat(10) { JoinTable(130, 130).save() }
            expectThrows(IllegalStateException::class, "too many rows matching JoinTable: 'customerId = :cid'{positional:{}, named:{cid:130}, finder:[]}") {
                JoinTable.dao.findSingleBy("customerId = :cid") { it.bind("cid", 130) }
            }
        }
    }
    group("findFirst() tests") {
        test("succeeds if there is exactly one entity") {
            val p = JoinTable(130, 130)
            p.save()
            expect(p) {
                JoinTable.dao.findFirst()
            }
        }

        test("returns null if there is no such entity") {
            expect(null) {
                JoinTable.dao.findFirst()
            }
        }

        test("returns random if there are two matching entities") {
            repeat(2) { JoinTable(130, 130).save() }
            expect(JoinTable(130, 130)) {
                JoinTable.dao.findFirst()
            }
        }
    }
    group("findFirstBy() tests") {
        test("succeeds if there is exactly one matching entity") {
            val p = JoinTable(130, 130)
            p.save()
            expect(p) {
                JoinTable.dao.findFirstBy("customerId = :cid") { it.bind("cid", 130) }
            }
        }

        test("returns null if there is no such entity") {
            expect(null) {
                JoinTable.dao.findFirstBy("customerId = :cid") { it.bind("cid", 130) }
            }
        }

        test("returns random if there are two matching entities") {
            repeat(2) { JoinTable(130, 130).save() }
            expect(JoinTable(130, 130)) {
                JoinTable.dao.findFirstBy("customerId = :cid") { it.bind("cid", 130) }
            }
        }
    }
    group("exists") {
        test("returns false on empty table") {
            expect(false) { JoinTable.dao.existsAny() }
            expect(false) { JoinTable.dao.existsBy("customerId=0") {} }
        }
        test("returns true on matching entity") {
            val p = JoinTable(100, 100)
            p.save()
            expect(true) { JoinTable.dao.existsAny() }
            expect(true) { JoinTable.dao.existsBy("customerId>=50") {} }
        }
        test("returns false on non-matching entity") {
            val p = JoinTable(100, 100)
            p.save()
            expect(false) { JoinTable.dao.existsBy("customerId>=200") {} }
        }
    }
}
