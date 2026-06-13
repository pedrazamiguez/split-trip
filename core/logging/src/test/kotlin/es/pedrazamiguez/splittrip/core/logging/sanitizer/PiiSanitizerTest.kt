package es.pedrazamiguez.splittrip.core.logging.sanitizer

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PiiSanitizerTest {

    @Test
    fun testMaskEmail() {
        assertEquals("j***e@e***e.com", "john.doe@example.com".maskEmail())
        assertEquals("a***@b***.com", "a@b.com".maskEmail())
        assertEquals("a***@c***.org", "ab@cd.org".maskEmail())
        assertEquals("not-an-email", "not-an-email".maskEmail())
    }

    @Test
    fun testSanitizePii() {
        assertEquals("User j***e@e***e.com logged in.", "User john.doe@example.com logged in.".sanitizePii())
        assertEquals("Contacts: a***@b***.com, a***@c***.org.", "Contacts: a@b.com, ab@cd.org.".sanitizePii())
        assertEquals("Normal message without email.", "Normal message without email.".sanitizePii())
    }
}
