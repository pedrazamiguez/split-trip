package es.pedrazamiguez.splittrip.domain.enums

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class GroupContextTest {

    @ParameterizedTest
    @EnumSource(GroupContext::class)
    fun `fromStringOrDefault resolves TRAVEL and EVERYDAY by exact name`(context: GroupContext) {
        assertEquals(context, GroupContext.fromStringOrDefault(context.name))
    }

    @Test
    fun `fromStringOrDefault resolves case-insensitive input`() {
        assertEquals(GroupContext.TRAVEL, GroupContext.fromStringOrDefault("travel"))
        assertEquals(GroupContext.EVERYDAY, GroupContext.fromStringOrDefault("everyday"))
    }

    @Test
    fun `fromStringOrDefault falls back to TRAVEL for null, blank, and unknown values`() {
        assertEquals(GroupContext.TRAVEL, GroupContext.fromStringOrDefault(null))
        assertEquals(GroupContext.TRAVEL, GroupContext.fromStringOrDefault(""))
        assertEquals(GroupContext.TRAVEL, GroupContext.fromStringOrDefault("   "))
        assertEquals(GroupContext.TRAVEL, GroupContext.fromStringOrDefault("UNKNOWN"))
    }
}
