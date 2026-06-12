package es.pedrazamiguez.splittrip.domain.exception

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class EmailCollisionExceptionTest {

    @Test
    fun `constructor sets fields correctly`() {
        val email = "collision@test.com"
        val cause = RuntimeException("Root cause")
        val exception = EmailCollisionException(email, cause)

        assertEquals(email, exception.email)
        assertEquals("An account with email collision@test.com already exists", exception.message)
        assertEquals(cause, exception.cause)
    }

    @Test
    fun `constructor handles null cause`() {
        val email = "collision@test.com"
        val exception = EmailCollisionException(email)

        assertEquals(email, exception.email)
        assertEquals("An account with email collision@test.com already exists", exception.message)
        assertNull(exception.cause)
    }
}
