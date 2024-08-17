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
import com.gitlab.mvysny.jdbiorm.quirks.DatabaseVariant
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.expect

/**
 * Tests Java functionality of the [Condition] API. These tests won't go to the database - see [AbstractFindByConditionTests] for that.
 */
class ConditionTest {
    @Nested inner class ToString {
        @Test fun simple() {
            expect("Person.id") { Person.ID.toString() }
        }
        @Test fun expressions() {
            expect("Person.id = 5") { Person.ID.eq(5L).toString() }
            expect("(NOT(Person.id = 5)) AND (Person.id = 6)") { Person.ID.eq(5L).not().and(Person.ID.eq(6L)).toString() }
            expect("Person.name ~ [foo]") { Person.NAME.fullTextMatches("foo").toString() }
        }
    }
    @Nested inner class NoConditionTest {
        @Test fun and() {
            expect(Condition.NO_CONDITION) { Condition.NO_CONDITION.and(null) }
            expect(Condition.NO_CONDITION) { Condition.NO_CONDITION.and(Condition.NO_CONDITION) }
            expect(Person.ID.eq(5L)) { Condition.NO_CONDITION.and(Person.ID.eq(5L)) }
            expect(Person.ID.eq(5L)) { Person.ID.eq(5L).and(Condition.NO_CONDITION) }
        }
        @Test fun or() {
            expect(Condition.NO_CONDITION) { Condition.NO_CONDITION.or(null) }
            expect(Condition.NO_CONDITION) { Condition.NO_CONDITION.or(Condition.NO_CONDITION) }
            expect(Person.ID.eq(5L)) { Condition.NO_CONDITION.or(Person.ID.eq(5L)) }
            expect(Person.ID.eq(5L)) { Person.ID.eq(5L).or(Condition.NO_CONDITION) }
        }
        @Test fun not() {
            expect(Condition.NO_CONDITION) { Condition.NO_CONDITION.not() }
        }
    }
    @Test fun `Condition is not @FunctionalInterface`() {
        // tests that Condition is not a FunctionalInterface.
        fun DaoOfAny<*>.deleteBy(block: String.() -> String) {}
        // if Condition is a functional interface, Kotlin will stubbornly use [DaoOfAny.deleteBy]
        // instead of the extension method defined above, and the code below will fail to compile.
        // This is problematic for vok-orm which can't then define its own extension methods using fancy {} syntax.
        DaoOfAny(Person::class.java).deleteBy { "" }
    }
    @Nested inner class ConditionTest {
        @Test fun isFalse() {
            expect(true) { IsFalse(Expression.Value(0)).test("ignored") }
            expect(true) { IsFalse(Expression.Value("0")).test("ignored") }
            expect(true) { IsFalse(Expression.Value("off")).test("ignored") }
            expect(false) { IsFalse(Expression.Value(1)).test("ignored") }
            expect(false) { IsFalse(Expression.Value(25)).test("ignored") }
        }
        @Test fun isTrue() {
            expect(false) { IsTrue(Expression.Value(0)).test("ignored") }
            expect(false) { IsTrue(Expression.Value("0")).test("ignored") }
            expect(false) { IsTrue(Expression.Value("off")).test("ignored") }
            expect(true) { IsTrue(Expression.Value(1)).test("ignored") }
            expect(true) { IsTrue(Expression.Value("on")).test("ignored") }
            expect(false) { IsTrue(Expression.Value(25)).test("ignored") }
        }
        @Test fun noCondition() {
            expect(true) { NoCondition.INSTANCE.test("ignored") }
        }
        @Test fun isNull() {
            expect(true) { IsNull(Expression.Value(null)).test("ignored") }
            expect(false) { IsNull(Expression.Value(4)).test("ignored") }
        }
        @Test fun isNotNull() {
            expect(false) { IsNotNull(Expression.Value(null)).test("ignored") }
            expect(true) { IsNotNull(Expression.Value(4)).test("ignored") }
        }
        @Test fun eq() {
            expect(false) { Eq(Expression.Value(null), Expression.Value(null)).test("ignored") }
            expect(false) { Eq(Expression.Value(null), Expression.Value(2)).test("ignored") }
            expect(false) { Eq(Expression.Value("2"), Expression.Value(2)).test("ignored") }
            expect(true) { Eq(Expression.Value("2"), Expression.Value("2")).test("ignored") }
        }
        @Test fun like() {
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
        @Nested inner class OpTest {
            @Test fun eq() {
                expect(false) { Op(Expression.Value(null), Expression.Value(null), Op.Operator.EQ).test("ignored") }
                expect(false) { Op(Expression.Value(null), Expression.Value(2), Op.Operator.EQ).test("ignored") }
                expect(false) { Op(Expression.Value("2"), Expression.Value(2), Op.Operator.EQ).test("ignored") }
                expect(true) { Op(Expression.Value("2"), Expression.Value("2"), Op.Operator.EQ).test("ignored") }
                expect(false) { Op(Expression.Value(1), Expression.Value(2), Op.Operator.EQ).test("ignored") }
                expect(false) { Op(Expression.Value(3), Expression.Value(2), Op.Operator.EQ).test("ignored") }
            }
            @Test fun lt() {
                expect(false) { Op(Expression.Value(null), Expression.Value(null), Op.Operator.LT).test("ignored") }
                expect(false) { Op(Expression.Value(null), Expression.Value(2), Op.Operator.LT).test("ignored") }
                expect(false) { Op(Expression.Value("2"), Expression.Value("2"), Op.Operator.LT).test("ignored") }
                expect(true) { Op(Expression.Value(1), Expression.Value(2), Op.Operator.LT).test("ignored") }
                expect(false) { Op(Expression.Value(3), Expression.Value(2), Op.Operator.LT).test("ignored") }
            }
            @Test fun le() {
                expect(false) { Op(Expression.Value(null), Expression.Value(null), Op.Operator.LE).test("ignored") }
                expect(false) { Op(Expression.Value(null), Expression.Value(2), Op.Operator.LE).test("ignored") }
                expect(true) { Op(Expression.Value("2"), Expression.Value("2"), Op.Operator.LE).test("ignored") }
                expect(true) { Op(Expression.Value(1), Expression.Value(2), Op.Operator.LE).test("ignored") }
                expect(false) { Op(Expression.Value(3), Expression.Value(2), Op.Operator.LE).test("ignored") }
            }
            @Test fun gt() {
                expect(false) { Op(Expression.Value(null), Expression.Value(null), Op.Operator.GT).test("ignored") }
                expect(false) { Op(Expression.Value(null), Expression.Value(2), Op.Operator.GT).test("ignored") }
                expect(false) { Op(Expression.Value("2"), Expression.Value("2"), Op.Operator.GT).test("ignored") }
                expect(false) { Op(Expression.Value(1), Expression.Value(2), Op.Operator.GT).test("ignored") }
                expect(true) { Op(Expression.Value(3), Expression.Value(2), Op.Operator.GT).test("ignored") }
            }
            @Test fun ge() {
                expect(false) { Op(Expression.Value(null), Expression.Value(null), Op.Operator.GE).test("ignored") }
                expect(false) { Op(Expression.Value(null), Expression.Value(2), Op.Operator.GE).test("ignored") }
                expect(true) { Op(Expression.Value("2"), Expression.Value("2"), Op.Operator.GE).test("ignored") }
                expect(false) { Op(Expression.Value(1), Expression.Value(2), Op.Operator.GE).test("ignored") }
                expect(true) { Op(Expression.Value(3), Expression.Value(2), Op.Operator.GE).test("ignored") }
            }
            @Test fun ne() {
                expect(false) { Op(Expression.Value(null), Expression.Value(null), Op.Operator.NE).test("ignored") }
                expect(false) { Op(Expression.Value(null), Expression.Value(2), Op.Operator.NE).test("ignored") }
                expect(true) { Op(Expression.Value("2"), Expression.Value(2), Op.Operator.NE).test("ignored") }
                expect(false) { Op(Expression.Value("2"), Expression.Value("2"), Op.Operator.NE).test("ignored") }
                expect(true) { Op(Expression.Value(1), Expression.Value(2), Op.Operator.NE).test("ignored") }
                expect(true) { Op(Expression.Value(3), Expression.Value(2), Op.Operator.NE).test("ignored") }
            }
        }
        @Test fun likeIgnoreCase() {
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
        @Test fun fullTextCondition() {
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
    @Nested inner class ExpressionCalculateTest {
        @Test fun value() {
            expect(null) { Expression.Value(null).calculate("ignored") }
            expect("Foo") { Expression.Value("Foo").calculate("ignored") }
            expect(5) { Expression.Value(5).calculate("ignored") }
        }
        @Test fun lower() {
            expect("foo") { Expression.Value("FOO").lower().calculate("ignored") }
            expect(null) { Expression.Value(null).lower().calculate("ignored") }
        }
        @Test fun coalesce() {
            expect("FOO") { Expression.Value("FOO").coalesce("foo").calculate("ignored") }
            expect(null) { Expression.Value<String>(null).coalesce(null).calculate("ignored") }
            expect("foo") { Expression.Value<String>("foo").coalesce("foo").calculate("ignored") }
            expect("foo") { Expression.Value<String>(null).coalesce("foo").calculate("ignored") }
            expect(null) { Expression.Value<String>(null).coalesce(null).calculate("ignored") }
        }
        @Test fun ifNull() {
            expect("FOO") { Expression.Value("FOO").ifNull("foo").calculate("ignored") }
            expect(null) { Expression.Value<String>(null).ifNull(null).calculate("ignored") }
            expect("foo") { Expression.Value<String>("foo").ifNull("foo").calculate("ignored") }
            expect("foo") { Expression.Value<String>(null).ifNull("foo").calculate("ignored") }
            expect(null) { Expression.Value<String>(null).ifNull(null).calculate("ignored") }
        }
        @Test fun nullIf() {
            expect("FOO") { Expression.Value("FOO").nullIf("foo").calculate("ignored") }
            expect(null) { Expression.Value<String>(null).nullIf("foo").calculate("ignored") }
            expect(null) { Expression.Value<String>("foo").nullIf("foo").calculate("ignored") }
            expect("foo") { Expression.Value<String>("foo").nullIf(null).calculate("ignored") }
            expect(null) { Expression.Value<String>(null).nullIf(null).calculate("ignored") }
        }
        @Test fun cast() {
            expect("FOO") { Expression.Value("FOO").castAsVarchar().calculate("ignored") }
            expect(null) { Expression.Value<String>(null).castAsVarchar().calculate("ignored") }
            expect("5") { Expression.Value(5).castAsVarchar().calculate("ignored") }
            expect("5") { Expression.Value(5L).castAsVarchar().calculate("ignored") }
            expect("true") { Expression.Value(true).castAsVarchar().calculate("ignored") }
        }
    }
    @Test fun nativeSQL() {
        expect("'name = :name'{name=foo}") { NativeSQL("name = :name", mapOf("name" to "foo")).toString() }
        expect("'name = :name'{name=foo}") { NativeSQL("name = :name", mapOf("name" to "foo")).toSql().toString() }
        expect("'name = :name AND age = :aaa'{aaa=25, name=foo}") { NativeSQL("name = :name AND age = :aaa", mapOf("name" to "foo", "aaa" to 25)).toString() }
        expect("'name = :name AND age = :aaa'{aaa=25, name=foo}") { NativeSQL("name = :name AND age = :aaa", mapOf("name" to "foo", "aaa" to 25)).toSql().toString() }
    }
}

/**
 * A test battery which tests conditions on an actual database.
 */
@DynaTestDsl
fun DynaNodeGroup.conditionTests(dbInfo: DatabaseInfo) {
}
