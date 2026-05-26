package es.pedrazamiguez.splittrip.data.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class JsonSanitizerTest {

    @Test
    fun `sanitize clean json does nothing`() {
        val input = """{"amount":"12.34","currency":"EUR"}"""
        val output = JsonSanitizer.sanitize(input)
        assertEquals(input, output)
    }

    @Test
    fun `sanitize removes code fences`() {
        val input = """
            ```json
            {"amount":"12.34"}
            ```
        """.trimIndent()
        val output = JsonSanitizer.sanitize(input)
        assertEquals("""{"amount":"12.34"}""", output)
    }

    @Test
    fun `sanitize removes code fences without language suffix`() {
        val input = """
            ```
            {"amount":"12.34"}
            ```
        """.trimIndent()
        val output = JsonSanitizer.sanitize(input)
        assertEquals("""{"amount":"12.34"}""", output)
    }

    @Test
    fun `sanitize strips conversational prefix and suffix`() {
        val input = """
            Here is the receipt data:
            {"amount":"12.34"}
            Hope this helps!
        """.trimIndent()
        val output = JsonSanitizer.sanitize(input)
        assertEquals("""{"amount":"12.34"}""", output)
    }

    @Test
    fun `sanitize strips complex noisy text with nested fences`() {
        val input = """
            Sure, here is your json block:
            ```json
            {"amount":"12.34","notes":"some notes"}
            ```
            Note that amount is in EUR.
        """.trimIndent()
        val output = JsonSanitizer.sanitize(input)
        assertEquals("""{"amount":"12.34","notes":"some notes"}""", output)
    }
}
