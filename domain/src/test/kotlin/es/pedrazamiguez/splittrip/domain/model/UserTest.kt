package es.pedrazamiguez.splittrip.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UserTest {

    @Test
    fun `generatePendingUserId produces deterministic hash matching firestore rules`() {
        val email = "jack.sparrow@example.com"
        val userId1 = User.generatePendingUserId(email)
        val userId2 = User.generatePendingUserId(" JACK.sparrow@example.com ")

        assertEquals(userId1, userId2)
        assertTrue(userId1.startsWith("pending_"))
        assertEquals(72, userId1.length)
        assertEquals(
            User.generatePendingUserId("jack.sparrow@example.com"),
            userId1
        )
    }

    @Test
    fun `normalizeEmail preserves dots and plus tags`() {
        assertEquals("pedraza.miguez@gmail.com", User.normalizeEmail("pedraza.miguez@gmail.com"))
        assertEquals("pedraza.miguez@gmail.com", User.normalizeEmail("  PEDRAZA.miguez@GMAIL.com  "))
        assertEquals("pedraza.miguez@googlemail.com", User.normalizeEmail("pedraza.miguez@googlemail.com"))
        assertEquals("pedraza.miguez+tag@gmail.com", User.normalizeEmail("pedraza.miguez+tag@gmail.com"))
        assertEquals("pedrazamiguez+tag@gmail.com", User.normalizeEmail("pedrazamiguez+tag@gmail.com"))
        assertEquals("pedraza.miguez@outlook.com", User.normalizeEmail("pedraza.miguez@outlook.com"))
        assertEquals("pedraza.miguez+tag@outlook.com", User.normalizeEmail("pedraza.miguez+tag@outlook.com"))
    }

    @Test
    fun `canonicalizeEmail normalizes Gmail and Googlemail addresses correctly`() {
        assertEquals("pedrazamiguez@gmail.com", User.canonicalizeEmail("pedraza.miguez@gmail.com"))
        assertEquals("pedrazamiguez@gmail.com", User.canonicalizeEmail("  PEDRAZA.miguez@GMAIL.com  "))
        assertEquals("pedrazamiguez@gmail.com", User.canonicalizeEmail("pedraza.miguez@googlemail.com"))
        assertEquals("pedrazamiguez@gmail.com", User.canonicalizeEmail("pedraza.miguez+tag@gmail.com"))
        assertEquals("pedrazamiguez@gmail.com", User.canonicalizeEmail("pedrazamiguez+tag@gmail.com"))
        assertEquals("pedraza.miguez@outlook.com", User.canonicalizeEmail("pedraza.miguez@outlook.com"))
        assertEquals("pedraza.miguez+tag@outlook.com", User.canonicalizeEmail("pedraza.miguez+tag@outlook.com"))
    }

    @Test
    fun `areEmailsEquivalent returns true for dotted and dotless Gmail addresses`() {
        assertTrue(User.areEmailsEquivalent("pedraza.miguez@gmail.com", "pedrazamiguez@gmail.com"))
        assertTrue(User.areEmailsEquivalent("p.e.d.r.a.z.a.m.i.g.u.e.z@gmail.com", "pedrazamiguez@gmail.com"))
        assertTrue(User.areEmailsEquivalent("pedraza.miguez+tag@gmail.com", "pedrazamiguez@gmail.com"))
        assertTrue(User.areEmailsEquivalent("pedraza.miguez@googlemail.com", "pedrazamiguez@gmail.com"))
    }

    @Test
    fun `areEmailsEquivalent returns false for distinct addresses`() {
        assertFalse(User.areEmailsEquivalent("pedraza@gmail.com", "miguez@gmail.com"))
        assertFalse(User.areEmailsEquivalent("pedraza.miguez@outlook.com", "pedrazamiguez@outlook.com"))
        assertFalse(User.areEmailsEquivalent("pedraza.miguez@gmail.com", "pedraza.miguez@outlook.com"))
    }

    @Test
    fun `generatePendingUserId produces distinct hashes for dotted and dotless Gmail addresses`() {
        val dotted = User.generatePendingUserId("pedraza.miguez@gmail.com")
        val dotless = User.generatePendingUserId("pedrazamiguez@gmail.com")
        assertNotEquals(dotted, dotless)
    }
}
