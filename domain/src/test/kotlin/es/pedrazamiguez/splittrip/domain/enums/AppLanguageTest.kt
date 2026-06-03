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
                Locale.setDefault(Locale("es"))
                assertEquals(AppLanguage.ES, AppLanguage.fromCode(null))

                Locale.setDefault(Locale("en"))
                assertEquals(AppLanguage.EN, AppLanguage.fromCode(null))

                Locale.setDefault(Locale("fr"))
                assertEquals(AppLanguage.EN, AppLanguage.fromCode(null))
            } finally {
                Locale.setDefault(originalLocale)
            }
        }
    }
}
