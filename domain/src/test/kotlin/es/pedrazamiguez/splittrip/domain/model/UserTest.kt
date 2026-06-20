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

    @Test
    fun `normalizeEmail normalizes Gmail and Googlemail addresses correctly`() {
        assertEquals("pedrazamiguez@gmail.com", User.normalizeEmail("pedraza.miguez@gmail.com"))
        assertEquals("pedrazamiguez@gmail.com", User.normalizeEmail("  PEDRAZA.miguez@GMAIL.com  "))
        assertEquals("pedrazamiguez@googlemail.com", User.normalizeEmail("pedraza.miguez@googlemail.com"))
        assertEquals("pedrazamiguez@gmail.com", User.normalizeEmail("pedraza.miguez+tag@gmail.com"))
        assertEquals("pedrazamiguez@gmail.com", User.normalizeEmail("pedrazamiguez+tag@gmail.com"))
    }

    @Test
    fun `normalizeEmail preserves non-Gmail addresses as is`() {
        assertEquals("pedraza.miguez@outlook.com", User.normalizeEmail("pedraza.miguez@outlook.com"))
        assertEquals("pedraza.miguez+tag@outlook.com", User.normalizeEmail("pedraza.miguez+tag@outlook.com"))
    }

    @Test
    fun `generatePendingUserId produces the same ID for equivalent Gmail addresses`() {
        val id1 = User.generatePendingUserId("pedraza.miguez@gmail.com")
        val id2 = User.generatePendingUserId("pedrazamiguez+tag@gmail.com")
        val id3 = User.generatePendingUserId("  PEDRAZA.miguez@GMAIL.com  ")

        assertEquals(id1, id2)
        assertEquals(id1, id3)
    }
}
