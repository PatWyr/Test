package com.gitlab.mvysny.jdbiorm.condition

import com.github.mvysny.dynatest.DynaNodeGroup
import com.github.mvysny.dynatest.DynaTest
import com.github.mvysny.dynatest.DynaTestDsl
import com.github.mvysny.dynatest.expectList
import com.gitlab.mvysny.jdbiorm.DaoOfAny
import com.gitlab.mvysny.jdbiorm.DatabaseInfo
import com.gitlab.mvysny.jdbiorm.JoinTable
import com.gitlab.mvysny.jdbiorm.Person
import com.gitlab.mvysny.jdbiorm.Person2
import kotlin.test.expect

/**
 * Tests Java functionality of the [Condition] API. These tests won't go to the database - see [conditionTests] for that.
 */
class ConditionTest : DynaTest({
    group("toString") {
        test("simple") {
            expect("Person.id") { Person.ID.toString() }
        }
        test("expressions") {
            expect("Person.id = 5") { Person.ID.eq(5L).toString() }
            expect("(NOT(Person.id = 5)) AND (Person.id = 6)") { Person.ID.eq(5L).not().and(Person.ID.eq(6L)).toString() }
            expect("Person.name ~ [foo]") { Person.NAME.fullTextMatches("foo").toString() }
        }
    }
    group("NoCondition") {
        test("and") {
            expect(Condition.NO_CONDITION) { Condition.NO_CONDITION.and(null) }
            expect(Condition.NO_CONDITION) { Condition.NO_CONDITION.and(Condition.NO_CONDITION) }
            expect(Person.ID.eq(5L)) { Condition.NO_CONDITION.and(Person.ID.eq(5L)) }
            expect(Person.ID.eq(5L)) { Person.ID.eq(5L).and(Condition.NO_CONDITION) }
        }
        test("or") {
            expect(Condition.NO_CONDITION) { Condition.NO_CONDITION.or(null) }
            expect(Condition.NO_CONDITION) { Condition.NO_CONDITION.or(Condition.NO_CONDITION) }
            expect(Person.ID.eq(5L)) { Condition.NO_CONDITION.or(Person.ID.eq(5L)) }
            expect(Person.ID.eq(5L)) { Person.ID.eq(5L).or(Condition.NO_CONDITION) }
        }
        test("not") {
            expect(Condition.NO_CONDITION) { Condition.NO_CONDITION.not() }
        }
    }
    test("Condition is not @FunctionalInterface") {
        // tests that Condition is not a FunctionalInterface.
        fun DaoOfAny<*>.deleteBy(block: String.() -> String) {}
        // if Condition is a functional interface, Kotlin will stubbornly use [DaoOfAny.deleteBy]
        // instead of the extension method defined above, and the code below will fail to compile.
        // This is problematic for vok-orm which can't then define its own extension methods using fancy {} syntax.
        DaoOfAny(Person::class.java).deleteBy { "" }
    }
    group("Condition.test") {
        test("IsFalse") {
            expect(true) { IsFalse(Expression.Value(0)).test("ignored") }
            expect(true) { IsFalse(Expression.Value("0")).test("ignored") }
            expect(true) { IsFalse(Expression.Value("off")).test("ignored") }
            expect(false) { IsFalse(Expression.Value(1)).test("ignored") }
            expect(false) { IsFalse(Expression.Value(25)).test("ignored") }
        }
        test("IsTrue") {
            expect(false) { IsTrue(Expression.Value(0)).test("ignored") }
            expect(false) { IsTrue(Expression.Value("0")).test("ignored") }
            expect(false) { IsTrue(Expression.Value("off")).test("ignored") }
            expect(true) { IsTrue(Expression.Value(1)).test("ignored") }
            expect(true) { IsTrue(Expression.Value("on")).test("ignored") }
            expect(false) { IsTrue(Expression.Value(25)).test("ignored") }
        }
        test("NoCondition") {
            expect(true) { NoCondition.INSTANCE.test("ignored") }
        }
        test("IsNull") {
            expect(true) { IsNull(Expression.Value(null)).test("ignored") }
            expect(false) { IsNull(Expression.Value(4)).test("ignored") }
        }
        test("IsNotNull") {
            expect(false) { IsNotNull(Expression.Value(null)).test("ignored") }
            expect(true) { IsNotNull(Expression.Value(4)).test("ignored") }
        }
        test("Eq") {
            expect(false) { Eq(Expression.Value(null), Expression.Value(null)).test("ignored") }
            expect(false) { Eq(Expression.Value(null), Expression.Value(2)).test("ignored") }
            expect(false) { Eq(Expression.Value("2"), Expression.Value(2)).test("ignored") }
            expect(true) { Eq(Expression.Value("2"), Expression.Value("2")).test("ignored") }
        }
        test("Like") {
            fun like(val1: Any?, val2: Any?) = Like(Expression.Value(val1), Expression.Value(val2)).test("ignored")
            expect(false) { like(null, "%") }
            expect(true) { like("a", "%") }
            expect(true) { like("a", "%a") }
            expect(false) { like("a", "%A") }
            expect(true) { like("a", "a") }
            expect(true) { like("a", "a%") }
            expect(false) { like("a", "A%") }
            expect(true) { like("alpha", "a%") }
            expect(false) { like("epsilon", "a%") }
            expect(true) { like("alpha", "%a") }
            expect(false) { like("epsilon", "%a") }
            expect(true) { like("alpha", "%a%") }
            expect(false) { like("epsilon", "%a%") }
            expect(true) { like("car", "%a%") }
            expect(true) { like("epsilon", "%psi%") }
            expect(true) { like("epsilon", "%epsi%") }
            expect(true) { like("epsilon", "%lon%") }
            expect(false) { like("epsilon", "%lan%") }
            expect(false) { like("epsilon", "%LON%") }
        }
        group("Op") {
            test("Eq") {
                expect(false) { Op(Expression.Value(null), Expression.Value(null), Op.Operator.EQ).test("ignored") }
                expect(false) { Op(Expression.Value(null), Expression.Value(2), Op.Operator.EQ).test("ignored") }
                expect(false) { Op(Expression.Value("2"), Expression.Value(2), Op.Operator.EQ).test("ignored") }
                expect(true) { Op(Expression.Value("2"), Expression.Value("2"), Op.Operator.EQ).test("ignored") }
                expect(false) { Op(Expression.Value(1), Expression.Value(2), Op.Operator.EQ).test("ignored") }
                expect(false) { Op(Expression.Value(3), Expression.Value(2), Op.Operator.EQ).test("ignored") }
            }
            test("Lt") {
                expect(false) { Op(Expression.Value(null), Expression.Value(null), Op.Operator.LT).test("ignored") }
                expect(false) { Op(Expression.Value(null), Expression.Value(2), Op.Operator.LT).test("ignored") }
                expect(false) { Op(Expression.Value("2"), Expression.Value("2"), Op.Operator.LT).test("ignored") }
                expect(true) { Op(Expression.Value(1), Expression.Value(2), Op.Operator.LT).test("ignored") }
                expect(false) { Op(Expression.Value(3), Expression.Value(2), Op.Operator.LT).test("ignored") }
            }
            test("LE") {
                expect(false) { Op(Expression.Value(null), Expression.Value(null), Op.Operator.LE).test("ignored") }
                expect(false) { Op(Expression.Value(null), Expression.Value(2), Op.Operator.LE).test("ignored") }
                expect(true) { Op(Expression.Value("2"), Expression.Value("2"), Op.Operator.LE).test("ignored") }
                expect(true) { Op(Expression.Value(1), Expression.Value(2), Op.Operator.LE).test("ignored") }
                expect(false) { Op(Expression.Value(3), Expression.Value(2), Op.Operator.LE).test("ignored") }
            }
            test("GT") {
                expect(false) { Op(Expression.Value(null), Expression.Value(null), Op.Operator.GT).test("ignored") }
                expect(false) { Op(Expression.Value(null), Expression.Value(2), Op.Operator.GT).test("ignored") }
                expect(false) { Op(Expression.Value("2"), Expression.Value("2"), Op.Operator.GT).test("ignored") }
                expect(false) { Op(Expression.Value(1), Expression.Value(2), Op.Operator.GT).test("ignored") }
                expect(true) { Op(Expression.Value(3), Expression.Value(2), Op.Operator.GT).test("ignored") }
            }
            test("GE") {
                expect(false) { Op(Expression.Value(null), Expression.Value(null), Op.Operator.GE).test("ignored") }
                expect(false) { Op(Expression.Value(null), Expression.Value(2), Op.Operator.GE).test("ignored") }
                expect(true) { Op(Expression.Value("2"), Expression.Value("2"), Op.Operator.GE).test("ignored") }
                expect(false) { Op(Expression.Value(1), Expression.Value(2), Op.Operator.GE).test("ignored") }
                expect(true) { Op(Expression.Value(3), Expression.Value(2), Op.Operator.GE).test("ignored") }
            }
            test("NE") {
                expect(false) { Op(Expression.Value(null), Expression.Value(null), Op.Operator.NE).test("ignored") }
                expect(false) { Op(Expression.Value(null), Expression.Value(2), Op.Operator.NE).test("ignored") }
                expect(true) { Op(Expression.Value("2"), Expression.Value(2), Op.Operator.NE).test("ignored") }
                expect(false) { Op(Expression.Value("2"), Expression.Value("2"), Op.Operator.NE).test("ignored") }
                expect(true) { Op(Expression.Value(1), Expression.Value(2), Op.Operator.NE).test("ignored") }
                expect(true) { Op(Expression.Value(3), Expression.Value(2), Op.Operator.NE).test("ignored") }
            }
        }
        test("LikeIgnoreCase") {
            fun ilike(val1: Any?, val2: Any?) = LikeIgnoreCase(Expression.Value(val1), Expression.Value(val2)).test("ignored")
            expect(false) { ilike(null, "%") }
            expect(true) { ilike("a", "%") }
            expect(true) { ilike("a", "%a") }
            expect(true) { ilike("a", "%A") }
            expect(true) { ilike("a", "a") }
            expect(true) { ilike("a", "a%") }
            expect(true) { ilike("a", "A%") }
            expect(true) { ilike("alpha", "a%") }
            expect(false) { ilike("epsilon", "a%") }
            expect(true) { ilike("alpha", "%a") }
            expect(false) { ilike("epsilon", "%a") }
            expect(true) { ilike("alpha", "%a%") }
            expect(false) { ilike("epsilon", "%a%") }
            expect(true) { ilike("car", "%a%") }
            expect(true) { ilike("epsilon", "%psi%") }
            expect(true) { ilike("epsilon", "%epsi%") }
            expect(true) { ilike("epsilon", "%lon%") }
            expect(false) { ilike("epsilon", "%lan%") }
            expect(true) { ilike("epsilon", "%LON%") }
            expect(false) { ilike("epsilon", "%LAN%") }
        }
        test("FullTextCondition") {
            fun ft(val1: Any?, query: String) = FullTextCondition.of(Expression.Value(val1), query).test("ignored")
            expect(false) { ft("", "foo") }
            expect(false) { ft(null, "foo") }
            expect(true) { ft(null, "") }
            expect(true) { ft("foo", "foo") }
            expect(true) { ft("fat cat", "c") }
            expect(true) { ft("fat cat", "cat") }
            expect(true) { ft("fat cat", "f c") }
            expect(false) { ft("fat cat", "f k") }
        }
    }
    group("Expression.calculate()") {
        test("Value") {
            expect(null) { Expression.Value(null).calculate("ignored") }
            expect("Foo") { Expression.Value("Foo").calculate("ignored") }
            expect(5) { Expression.Value(5).calculate("ignored") }
        }
        test("Lower") {
            expect("foo") { Expression.Value("FOO").lower().calculate("ignored") }
            expect(null) { Expression.Value(null).lower().calculate("ignored") }
        }
        test("Coalesce") {
            expect("FOO") { Expression.Value("FOO").coalesce("foo").calculate("ignored") }
            expect("foo") { Expression.Value<String>(null).coalesce("foo").calculate("ignored") }
        }
    }
})

/**
 * A test battery which tests conditions on an actual database.
 */
@DynaTestDsl
fun DynaNodeGroup.conditionTests(dbInfo: DatabaseInfo) {
    // check that the produced SQL actually executes and is accepted by the database. We don't test the
    // correctness of the result just yet
    group("smoke") {
        test("simple conditions") {
            JoinTable.dao.findAllBy(JoinTable.ORDERID.eq(1))
            JoinTable.dao.findAllBy(JoinTable.ORDERID.eq(JoinTable.CUSTOMERID))
            JoinTable.dao.findAllBy(JoinTable.ORDERID.lt(1))
            JoinTable.dao.findAllBy(JoinTable.ORDERID.lt(JoinTable.CUSTOMERID))
            JoinTable.dao.findAllBy(JoinTable.ORDERID.le(1))
            JoinTable.dao.findAllBy(JoinTable.ORDERID.le(JoinTable.CUSTOMERID))
            JoinTable.dao.findAllBy(JoinTable.ORDERID.gt(1))
            JoinTable.dao.findAllBy(JoinTable.ORDERID.gt(JoinTable.CUSTOMERID))
            JoinTable.dao.findAllBy(JoinTable.ORDERID.ge(1))
            JoinTable.dao.findAllBy(JoinTable.ORDERID.ge(JoinTable.CUSTOMERID))
            JoinTable.dao.findAllBy(JoinTable.ORDERID.ne(1))
            JoinTable.dao.findAllBy(JoinTable.ORDERID.ne(JoinTable.CUSTOMERID))
            JoinTable.dao.findAllBy(JoinTable.ORDERID.`in`(1, 2, 3))
            JoinTable.dao.findAllBy(JoinTable.ORDERID.`in`(JoinTable.CUSTOMERID))
            JoinTable.dao.findAllBy(JoinTable.ORDERID.notIn(1, 2, 3))
            JoinTable.dao.findAllBy(JoinTable.ORDERID.notIn(JoinTable.CUSTOMERID))
            JoinTable.dao.findAllBy(JoinTable.ORDERID.between(1, 10))
            JoinTable.dao.findAllBy(JoinTable.ORDERID.notBetween(1, 10))
            Person.dao.findAllBy(Person.ISALIVE25.isTrue())
            Person.dao.findAllBy(Person.ISALIVE25.isFalse())
            Person.dao.findAllBy(Person.NAME.equalIgnoreCase("Foo"))
            Person.dao.findAllBy(Person.NAME.notEqualIgnoreCase("Bar"))
            Person.dao.findAllBy(Person.NAME.like("%Bar"))
            Person.dao.findAllBy(Person.NAME.likeIgnoreCase("%Baz"))
            Person.dao.findAllBy(Person.NAME.isNull())
            Person.dao.findAllBy(Person.NAME.isNotNull())
        }
        test("combining") {
            JoinTable.dao.findAllBy(JoinTable.ORDERID.eq(1).and(JoinTable.CUSTOMERID.eq(2)))
            JoinTable.dao.findAllBy(JoinTable.ORDERID.eq(1).or(JoinTable.CUSTOMERID.eq(2)))
            JoinTable.dao.findAllBy(JoinTable.ORDERID.eq(1).and(JoinTable.CUSTOMERID.eq(2)).not())
        }
    }
    test("eq") {
        val person = Person2(name = "Foo", age = 25, isAlive25 = true)
        person.save()
        expectList() { Person2.dao.findAllBy(Person2.NAME.eq("Bar")) }
        expectList(person) { Person2.dao.findAllBy(Person2.NAME.eq("Foo")) }
        expectList() { Person2.dao.findAllBy(Person2.AGE.eq(2)) }
        expectList(person) { Person2.dao.findAllBy(Person2.AGE.eq(25)) }
        expectList() { Person2.dao.findAllBy(Person2.ISALIVE25.eq(false)) }
        expectList(person) { Person2.dao.findAllBy(Person2.ISALIVE25.eq(true)) }
    }
    test("lt") {
        val person = Person2(name = "Foo", age = 25)
        person.save()
        expectList() { Person2.dao.findAllBy(Person2.NAME.lt("Bar")) }
        expectList() { Person2.dao.findAllBy(Person2.NAME.lt("Foo")) }
        expectList(person) { Person2.dao.findAllBy(Person2.NAME.lt("ZZZ")) }
        expectList() { Person2.dao.findAllBy(Person2.AGE.lt(2)) }
        expectList() { Person2.dao.findAllBy(Person2.AGE.lt(25)) }
        expectList(person) { Person2.dao.findAllBy(Person2.AGE.lt(100)) }
    }
    test("le") {
        val person = Person2(name = "Foo", age = 25)
        person.save()
        expectList() { Person2.dao.findAllBy(Person2.NAME.le("Bar")) }
        expectList(person) { Person2.dao.findAllBy(Person2.NAME.le("Foo")) }
        expectList(person) { Person2.dao.findAllBy(Person2.NAME.le("ZZZ")) }
        expectList() { Person2.dao.findAllBy(Person2.AGE.le(2)) }
        expectList(person) { Person2.dao.findAllBy(Person2.AGE.le(25)) }
        expectList(person) { Person2.dao.findAllBy(Person2.AGE.le(100)) }
    }
    test("gt") {
        val person = Person2(name = "Foo", age = 25)
        person.save()
        expectList(person) { Person2.dao.findAllBy(Person2.NAME.gt("Bar")) }
        expectList() { Person2.dao.findAllBy(Person2.NAME.gt("Foo")) }
        expectList() { Person2.dao.findAllBy(Person2.NAME.gt("ZZZ")) }
        expectList(person) { Person2.dao.findAllBy(Person2.AGE.gt(2)) }
        expectList() { Person2.dao.findAllBy(Person2.AGE.gt(25)) }
        expectList() { Person2.dao.findAllBy(Person2.AGE.gt(100)) }
    }
    test("ge") {
        val person = Person2(name = "Foo", age = 25)
        person.save()
        expectList(person) { Person2.dao.findAllBy(Person2.NAME.ge("Bar")) }
        expectList(person) { Person2.dao.findAllBy(Person2.NAME.ge("Foo")) }
        expectList() { Person2.dao.findAllBy(Person2.NAME.ge("ZZZ")) }
        expectList(person) { Person2.dao.findAllBy(Person2.AGE.ge(2)) }
        expectList(person) { Person2.dao.findAllBy(Person2.AGE.ge(25)) }
        expectList() { Person2.dao.findAllBy(Person2.AGE.ge(100)) }
    }
    test("ne") {
        val person = Person2(name = "Foo", age = 25, isAlive25 = false)
        person.save()
        expectList(person) { Person2.dao.findAllBy(Person2.NAME.ne("Bar")) }
        expectList() { Person2.dao.findAllBy(Person2.NAME.ne("Foo")) }
        expectList(person) { Person2.dao.findAllBy(Person2.NAME.ne("ZZZ")) }
        expectList(person) { Person2.dao.findAllBy(Person2.AGE.ne(2)) }
        expectList() { Person2.dao.findAllBy(Person2.AGE.ne(25)) }
        expectList(person) { Person2.dao.findAllBy(Person2.AGE.ne(100)) }
        expectList() { Person2.dao.findAllBy(Person2.ISALIVE25.ne(false)) }
        expectList(person) { Person2.dao.findAllBy(Person2.ISALIVE25.ne(true)) }
    }
    test("in") {
        val person = Person2(name = "Foo", age = 25, isAlive25 = false)
        person.save()
        expectList() { Person2.dao.findAllBy(Person2.NAME.`in`("Bar")) }
        expectList(person) { Person2.dao.findAllBy(Person2.NAME.`in`("Foo", "Bar")) }
        expectList() { Person2.dao.findAllBy(Person2.AGE.`in`(2)) }
        expectList(person) { Person2.dao.findAllBy(Person2.AGE.`in`(2, 25)) }
        expectList(person) { Person2.dao.findAllBy(Person2.AGE.`in`(2, 25, 100)) }
        expectList() { Person2.dao.findAllBy(Person2.ISALIVE25.`in`(true)) }
        expectList(person) { Person2.dao.findAllBy(Person2.ISALIVE25.`in`(true, false)) }
    }
    test("notIn") {
        val person = Person2(name = "Foo", age = 25, isAlive25 = false)
        person.save()
        expectList(person) { Person2.dao.findAllBy(Person2.NAME.notIn("Bar")) }
        expectList() { Person2.dao.findAllBy(Person2.NAME.notIn("Foo", "Bar")) }
        expectList(person) { Person2.dao.findAllBy(Person2.AGE.notIn(2)) }
        expectList() { Person2.dao.findAllBy(Person2.AGE.notIn(2, 25)) }
        expectList() { Person2.dao.findAllBy(Person2.AGE.notIn(2, 25, 100)) }
        expectList(person) { Person2.dao.findAllBy(Person2.ISALIVE25.notIn(true)) }
        expectList() { Person2.dao.findAllBy(Person2.ISALIVE25.notIn(true, false)) }
    }
    test("between") {
        val person = Person2(name = "Foo", age = 25, isAlive25 = false)
        person.save()
        expectList() { Person2.dao.findAllBy(Person2.AGE.between(2, 3)) }
        expectList(person) { Person2.dao.findAllBy(Person2.AGE.between(2, 25)) }
        expectList(person) { Person2.dao.findAllBy(Person2.AGE.between(2, 100)) }
    }
    test("notBetween") {
        val person = Person2(name = "Foo", age = 25, isAlive25 = false)
        person.save()
        expectList(person) { Person2.dao.findAllBy(Person2.AGE.notBetween(2, 3)) }
        expectList() { Person2.dao.findAllBy(Person2.AGE.notBetween(2, 25)) }
        expectList() { Person2.dao.findAllBy(Person2.AGE.notBetween(2, 100)) }
    }
    test("isTrue") {
        val person = Person2(name = "Foo", age = 25, isAlive25 = false)
        person.save()
        expectList() { Person2.dao.findAllBy(Person2.ISALIVE25.isTrue()) }
        person.isAlive25 = true
        person.save()
        expectList(person) { Person2.dao.findAllBy(Person2.ISALIVE25.isTrue()) }
    }
    test("isFalse") {
        val person = Person2(name = "Foo", age = 25, isAlive25 = false)
        person.save()
        expectList(person) { Person2.dao.findAllBy(Person2.ISALIVE25.isFalse()) }
        person.isAlive25 = true
        person.save()
        expectList() { Person2.dao.findAllBy(Person2.ISALIVE25.isFalse()) }
    }
    test("equalIgnoreCase") {
        val person = Person2(name = "Foo", age = 25, isAlive25 = true)
        person.save()
        expectList() { Person2.dao.findAllBy(Person2.NAME.equalIgnoreCase("Bar")) }
        expectList(person) { Person2.dao.findAllBy(Person2.NAME.equalIgnoreCase("foo")) }
        expectList(person) { Person2.dao.findAllBy(Person2.NAME.equalIgnoreCase("Foo")) }
    }
    test("notEqualIgnoreCase") {
        val person = Person2(name = "Foo", age = 25, isAlive25 = true)
        person.save()
        expectList(person) { Person2.dao.findAllBy(Person2.NAME.notEqualIgnoreCase("Bar")) }
        expectList() { Person2.dao.findAllBy(Person2.NAME.notEqualIgnoreCase("foo")) }
        expectList() { Person2.dao.findAllBy(Person2.NAME.notEqualIgnoreCase("Foo")) }
    }
    test("like") {
        val person = Person2(name = "Foo", age = 25, isAlive25 = true)
        person.save()
        expectList() { Person2.dao.findAllBy(Person2.NAME.like("%Bar")) }
        // MariaDB matches case-insensitive, H2 does not.
//        expectList() { Person2.dao.findAllBy(Person2.NAME.like("%foo")) }
        expectList(person) { Person2.dao.findAllBy(Person2.NAME.like("%Foo")) }
        expectList(person) { Person2.dao.findAllBy(Person2.NAME.like("Foo")) }
        expectList() { Person2.dao.findAllBy(Person2.NAME.like("o")) }
    }
    test("likeIgnoreCase") {
        val person = Person2(name = "Foo", age = 25, isAlive25 = true)
        person.save()
        expectList() { Person2.dao.findAllBy(Person2.NAME.likeIgnoreCase("%Bar")) }
        expectList(person) { Person2.dao.findAllBy(Person2.NAME.likeIgnoreCase("%foo")) }
        expectList(person) { Person2.dao.findAllBy(Person2.NAME.likeIgnoreCase("%Foo")) }
        expectList(person) { Person2.dao.findAllBy(Person2.NAME.likeIgnoreCase("Foo")) }
        expectList(person) { Person2.dao.findAllBy(Person2.NAME.likeIgnoreCase("foo")) }
        expectList() { Person2.dao.findAllBy(Person2.NAME.likeIgnoreCase("o")) }
    }
    test("isNull") {
        val person = Person2(name = "Foo", age = 25)
        person.save()
        expectList(person) { Person2.dao.findAllBy(Person2.ISALIVE25.isNull()) }
        person.isAlive25 = true
        person.save()
        expectList() { Person2.dao.findAllBy(Person2.ISALIVE25.isNull()) }
    }
    test("isNotNull") {
        val person = Person2(name = "Foo", age = 25)
        person.save()
        expectList() { Person2.dao.findAllBy(Person2.ISALIVE25.isNotNull()) }
        person.isAlive25 = true
        person.save()
        expectList(person) { Person2.dao.findAllBy(Person2.ISALIVE25.isNotNull()) }
    }
    group("no condition") {
        test("findAllBy") {
            val person = Person2(name = "Foo", age = 25)
            person.save()
            expectList(person) { Person2.dao.findAllBy(null) }
            expectList(person) { Person2.dao.findAllBy(Condition.NO_CONDITION) }
        }
        test("deleteAllBy null") {
            val person = Person2(name = "Foo", age = 25)
            person.save()
            Person2.dao.deleteBy(null)
            expectList() { Person2.dao.findAll() }
        }
        test("deleteAllBy no condition") {
            val person = Person2(name = "Foo", age = 25)
            person.save()
            Person2.dao.deleteBy(Condition.NO_CONDITION)
            expectList() { Person2.dao.findAll() }
        }
        test("exists by") {
            expect(false) { Person2.dao.existsBy(null) }
            expect(false) { Person2.dao.existsBy(Condition.NO_CONDITION) }
            val person = Person2(name = "Foo", age = 25)
            person.save()
            expect(true) { Person2.dao.existsBy(null) }
            expect(true) { Person2.dao.existsBy(Condition.NO_CONDITION) }
        }
        test("countBy") {
            expect(0) { Person2.dao.countBy(null) }
            expect(0) { Person2.dao.countBy(Condition.NO_CONDITION) }
            val person = Person2(name = "Foo", age = 25)
            person.save()
            expect(1) { Person2.dao.countBy(null) }
            expect(1) { Person2.dao.countBy(Condition.NO_CONDITION) }
        }
    }
    test("Coalesce") {
        Person2(name = "Foo", age = 25).save()
        expect(1) { Person2.dao.countBy(Person2.SOMESTRINGVALUE.coalesce("Foo").eq("Foo")) }
        Person2(name = "Foo", age = 25, someStringValue = "Bar").save()
        expect(1) { Person2.dao.countBy(Person2.SOMESTRINGVALUE.coalesce("Foo").eq("Foo")) }
        expect(1) { Person2.dao.countBy(Person2.SOMESTRINGVALUE.coalesce("Foo").eq("Bar")) }
        expect(0) { Person2.dao.countBy(Person2.SOMESTRINGVALUE.coalesce("Foo").eq("Baz")) }
    }
    group("full-text search") {
        test("construct sql succeeds") {
            Person2.NAME.fullTextMatches("foo").toSql()
        }

        if (dbInfo.supportsFullText) {
            test("smoke test") {
                Person.findAllBy(Person.NAME.fullTextMatches(""))
                Person.findAllBy(Person.NAME.fullTextMatches("a"))
                Person.findAllBy(Person.NAME.fullTextMatches("the"))
                Person.findAllBy(Person.NAME.fullTextMatches("Moby"))
            }

            test("blank filter matches all records") {
                val moby = Person(name = "Moby")
                moby.create()
                expectList(moby) { Person.findAllBy(Person.NAME.fullTextMatches("")) }
            }

            test("various queries matching/not matching Moby") {
                val moby = Person(name = "Moby")
                moby.create()
                expectList() { Person.findAllBy(Person.NAME.fullTextMatches("foobar")) }
                expectList(moby) { Person.findAllBy(Person.NAME.fullTextMatches("Moby")) }
                expectList() { Person.findAllBy(Person.NAME.fullTextMatches("Jerry")) }
                expectList() { Person.findAllBy(Person.NAME.fullTextMatches("Jerry Moby")) }
            }

            test("partial match") {
                val moby = Person(name = "Moby")
                moby.create()
                expectList(moby) { Person.findAllBy(Person.NAME.fullTextMatches("Mob")) }
            }
        }
    }
}
