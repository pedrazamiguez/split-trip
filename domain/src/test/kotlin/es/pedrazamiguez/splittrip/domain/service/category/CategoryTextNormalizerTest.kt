package es.pedrazamiguez.splittrip.domain.service.category

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class CategoryTextNormalizerTest {

    @Test
    fun `strips diacritics, punctuation, and extra whitespace while lowercasing`() {
        assertEquals("cafe y tostada", CategoryTextNormalizer.normalize("Café y Tostada!"))
        assertEquals("7 eleven", CategoryTextNormalizer.normalize("7-Eleven"))
        assertEquals("peluqueria maria", CategoryTextNormalizer.normalize("  Peluquería   María  "))
    }

    @Test
    fun `returns null for blank or punctuation-only strings`() {
        assertNull(CategoryTextNormalizer.normalize(""))
        assertNull(CategoryTextNormalizer.normalize("   "))
        assertNull(CategoryTextNormalizer.normalize("!!! ---"))
    }

    @Test
    fun `is idempotent on already-normalized strings`() {
        val samples = listOf(
            "Café y Tostada!",
            "7-Eleven",
            "Peluquería María",
            "Alquiler de coche (aeropuerto)"
        )
        samples.forEach { raw ->
            val first = CategoryTextNormalizer.normalize(raw)!!
            val second = CategoryTextNormalizer.normalize(first)
            assertEquals(first, second)
        }
    }
}
