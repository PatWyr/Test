package com.gitlab.mvysny.jdbiorm

import com.github.mvysny.dynatest.DynaTest
import com.github.mvysny.dynatest.cloneBySerialization
import kotlin.test.expect

class EntityMetaTest : DynaTest({
    test("serialization") {
        expect(EntityMeta.of(JoinTable::class.java)) { EntityMeta.of(JoinTable::class.java).cloneBySerialization() }
    }
})
