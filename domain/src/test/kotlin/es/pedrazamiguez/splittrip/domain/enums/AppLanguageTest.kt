package es.pedrazamiguez.splittrip.domain.enums

import java.util.Locale
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class AppLanguageTest {

    @Nested
    inner class FromCode {

        @ParameterizedTest
        @CsvSource(
            "en, EN",
            "es, ES",
            "es-x-andaluh, ANDALUZ",
            "es-X-Andaluh, ANDALUZ",
            "es-rAN, ANDALUZ",
            "es-AN, ANDALUZ",
            "es-an, ANDALUZ",
            "EN, EN",
            "ES, ES",
            "En, EN",
            "Es, ES"
        )
        fun `parses known codes case-insensitively`(input: String, expected: AppLanguage) {
            assertEquals(expected, AppLanguage.fromCode(input))
        }

        @Test
        fun `returns EN for unknown language code`() {
            assertEquals(AppLanguage.EN, AppLanguage.fromCode("fr"))
            assertEquals(AppLanguage.EN, AppLanguage.fromCode("de"))
            assertEquals(AppLanguage.EN, AppLanguage.fromCode(""))
        }

        @Test
        fun `falls back to system locale for null input`() {
            val originalLocale = Locale.getDefault()
            try {
                Locale.setDefault(Locale.forLanguageTag("es"))
                assertEquals(AppLanguage.ES, AppLanguage.fromCode(null))

                Locale.setDefault(Locale.forLanguageTag("en"))
                assertEquals(AppLanguage.EN, AppLanguage.fromCode(null))

                Locale.setDefault(Locale.forLanguageTag("es-x-andaluh"))
                assertEquals(AppLanguage.ANDALUZ, AppLanguage.fromCode(null))

                Locale.setDefault(Locale.forLanguageTag("es-AN"))
                assertEquals(AppLanguage.ANDALUZ, AppLanguage.fromCode(null))

                Locale.setDefault(Locale.forLanguageTag("fr"))
                assertEquals(AppLanguage.EN, AppLanguage.fromCode(null))
            } finally {
                Locale.setDefault(originalLocale)
            }
        }
    }

    @Nested
    inner class ResolveBaseLanguageCode {

        @ParameterizedTest
        @CsvSource(
            "en, en",
            "es, es",
            "es-ES, es",
            "es_MX, es",
            "es-AN, es",
            "es-rAN, es",
            "es-x-andaluh, es"
        )
        fun `maps EN, ES, and Andaluz variants to base language codes`(input: String, expected: String) {
            assertEquals(expected, AppLanguage.resolveBaseLanguageCode(input))
        }

        @ParameterizedTest
        @CsvSource(
            "fr, fr",
            "fr-FR, fr",
            "pt-BR, pt",
            "DE, de"
        )
        fun `preserves extensible non-enum ISO language codes`(input: String, expected: String) {
            assertEquals(expected, AppLanguage.resolveBaseLanguageCode(input))
        }

        @Test
        fun `falls back to system default locale for null or blank input`() {
            val originalLocale = Locale.getDefault()
            try {
                Locale.setDefault(Locale.forLanguageTag("es"))
                assertEquals("es", AppLanguage.resolveBaseLanguageCode(null))
                assertEquals("es", AppLanguage.resolveBaseLanguageCode("   "))

                Locale.setDefault(Locale.forLanguageTag("en"))
                assertEquals("en", AppLanguage.resolveBaseLanguageCode(null))

                Locale.setDefault(Locale.forLanguageTag("es-AN"))
                assertEquals("es", AppLanguage.resolveBaseLanguageCode(null))

                Locale.setDefault(Locale.forLanguageTag("fr"))
                assertEquals("fr", AppLanguage.resolveBaseLanguageCode(null))
            } finally {
                Locale.setDefault(originalLocale)
            }
        }

        @Test
        fun `baseLanguageCode property matches expected base codes for all enum entries`() {
            assertEquals("en", AppLanguage.EN.baseLanguageCode)
            assertEquals("es", AppLanguage.ES.baseLanguageCode)
            assertEquals("es", AppLanguage.ANDALUZ.baseLanguageCode)
        }
    }
}
