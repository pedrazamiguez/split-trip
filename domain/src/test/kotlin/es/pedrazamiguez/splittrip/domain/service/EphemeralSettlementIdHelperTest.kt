package es.pedrazamiguez.splittrip.domain.service

import es.pedrazamiguez.splittrip.domain.model.Settlement
import es.pedrazamiguez.splittrip.domain.model.SettlementPocketType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EphemeralSettlementIdHelperTest {

    @Test
    fun `createId formats valid prefixed payload`() {
        val id = EphemeralSettlementIdHelper.createId(
            fromUserId = "user1",
            toUserId = "user2",
            sourcePocket = SettlementPocketType.POCKET,
            currency = "EUR",
            amount = 5000L
        )

        assertEquals("ephemeral|user1|user2|POCKET|EUR|5000", id)
        assertTrue(EphemeralSettlementIdHelper.isEphemeral(id))
    }

    @Test
    fun `parse decodes valid ephemeral ID`() {
        val parsed = EphemeralSettlementIdHelper.parse("ephemeral|user1|user2|POCKET|EUR|5000")

        assertNotNull(parsed)
        assertEquals(
            Settlement(
                fromUserId = "user1",
                toUserId = "user2",
                amount = 5000L,
                currency = "EUR",
                sourcePocket = SettlementPocketType.POCKET
            ),
            parsed
        )
    }

    @Test
    fun `parse handles unregistered member IDs with underscores`() {
        val parsed = EphemeralSettlementIdHelper.parse("ephemeral|pending_uuid-1|pending_uuid-2|POCKET|EUR|1000")

        assertNotNull(parsed)
        assertEquals("pending_uuid-1", parsed?.fromUserId)
        assertEquals("pending_uuid-2", parsed?.toUserId)
        assertEquals(1000L, parsed?.amount)
        assertEquals("EUR", parsed?.currency)
        assertEquals(SettlementPocketType.POCKET, parsed?.sourcePocket)
    }

    @Test
    fun `isEphemeral returns false for standard UUIDs`() {
        val isEphemeral = EphemeralSettlementIdHelper.isEphemeral("550e8400-e29b-41d4-a716-446655440000")
        assertFalse(isEphemeral)
    }

    @Test
    fun `parse returns null for invalid formats`() {
        assertNull(EphemeralSettlementIdHelper.parse("not-ephemeral"))
        assertNull(EphemeralSettlementIdHelper.parse("ephemeral|user1|user2"))
        assertNull(EphemeralSettlementIdHelper.parse("ephemeral|user1|user2|INVALID_POCKET|EUR|5000"))
        assertNull(EphemeralSettlementIdHelper.parse("ephemeral|user1|user2|POCKET|EUR|not-a-number"))
    }
}
