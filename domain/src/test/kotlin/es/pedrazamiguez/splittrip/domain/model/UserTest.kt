package es.pedrazamiguez.splittrip.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UserTest {

    @Test
    fun `generatePendingUserId returns deterministic hash prefixed with pending_`() {
        val email = "jack@example.com"
        val userId1 = User.generatePendingUserId(email)
        val userId2 = User.generatePendingUserId(" JACK@example.com ")

        assertEquals(userId1, userId2)
        assertTrue(userId1.startsWith("pending_"))
        assertEquals(72, userId1.length)
    }
}
