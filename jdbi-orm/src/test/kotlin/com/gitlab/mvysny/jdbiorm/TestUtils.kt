package com.gitlab.mvysny.jdbiorm

import com.fatboyindustrial.gsonjavatime.Converters
import com.gitlab.mvysny.jdbiorm.JdbiOrm.jdbi
import com.google.gson.*
import org.intellij.lang.annotations.Language
import org.jdbi.v3.core.Handle
import kotlin.test.expect

private fun GsonBuilder.registerJavaTimeAdapters(): GsonBuilder = apply {
    Converters.registerAll(this)
}

val gson: Gson = GsonBuilder()
    .registerJavaTimeAdapters()
    .create()

fun Handle.ddl(@Language("sql") sql: String) {
    execute(sql)
}

fun clearDb() {
    Person.deleteAll()
    EntityWithAliasedId.dao.deleteAll()
    NaturalPerson.deleteAll()
    LogRecord.deleteAll()
    TypeMappingEntity.deleteAll()
    JoinTable.dao.deleteAll()
    MappingTable.dao.deleteAll()
}

fun <T> db(block: Handle.() -> T): T = jdbi().inTransaction<T, Exception>(block)

val isX86_64: Boolean get() = System.getProperty("os.arch") == "amd64"

/**
 * Expects that [actual] list of objects matches [expected] list of objects. Fails otherwise.
 */
fun <T> expectList(vararg expected: T, actual: ()->List<T>) {
    expect(expected.toList(), actual)
}
