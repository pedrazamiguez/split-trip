package es.pedrazamiguez.splittrip.data.local.converter

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("StringListConverter")
class StringListConverterTest {

    private val converter = StringListConverter()

    @Nested
    inner class FromStringList {

        @Test
        fun `serializes a list of strings into a delimited string`() {
            val result = converter.fromStringList(listOf("EUR", "USD", "THB"))
            assertEquals("EUR|||USD|||THB", result)
        }

        @Test
        fun `serializes a single-element list`() {
            val result = converter.fromStringList(listOf("EUR"))
            assertEquals("EUR", result)
        }

        @Test
        fun `serializes an empty list to an empty string`() {
            val result = converter.fromStringList(emptyList())
            assertEquals("", result)
        }
    }

    @Nested
    inner class ToStringList {

        @Test
        fun `deserializes a delimited string back to the original list`() {
            val result = converter.toStringList("EUR|||USD|||THB")
            assertEquals(listOf("EUR", "USD", "THB"), result)
        }

        @Test
        fun `deserializes a single value to a one-element list`() {
            val result = converter.toStringList("EUR")
            assertEquals(listOf("EUR"), result)
        }

        @Test
        fun `returns an empty list for an empty string`() {
            val result = converter.toStringList("")
            assertTrue(result.isEmpty())
        }
    }

    @Nested
    inner class RoundTrip {

        @Test
        fun `round-trips a non-empty list without data loss`() {
            val original = listOf("alice@example.com", "bob@example.com", "carol@example.com")
            val serialized = converter.fromStringList(original)
            val deserialized = converter.toStringList(serialized)
            assertEquals(original, deserialized)
        }

        @Test
        fun `round-trips an empty list without data loss`() {
            val original = emptyList<String>()
            val serialized = converter.fromStringList(original)
            val deserialized = converter.toStringList(serialized)
            assertEquals(original, deserialized)
        }
    }
}
