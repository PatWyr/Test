package com.gitlab.mvysny.jdbiorm

import org.junit.jupiter.api.Test
import kotlin.test.expect

class EntityMetaTest {
    @Test fun serialization() {
        expect(EntityMeta.of(JoinTable::class.java)) { EntityMeta.of(JoinTable::class.java).cloneBySerialization() }
    }
}
