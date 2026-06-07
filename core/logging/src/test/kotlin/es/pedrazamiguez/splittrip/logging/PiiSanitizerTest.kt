package es.pedrazamiguez.splittrip.logging

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
}
