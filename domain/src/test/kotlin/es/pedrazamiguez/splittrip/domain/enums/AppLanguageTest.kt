package es.pedrazamiguez.splittrip.domain.enums

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
    }
}
