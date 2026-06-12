package es.pedrazamiguez.splittrip.features.settings.presentation.mapper.impl

import es.pedrazamiguez.splittrip.core.common.provider.LocaleProvider
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDateTime
import java.util.Locale
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AccountStatusUiMapperImplTest {

    private lateinit var localeProvider: LocaleProvider
    private lateinit var mapper: AccountStatusUiMapperImpl

    @BeforeEach
    fun setUp() {
        localeProvider = mockk()
        every { localeProvider.getCurrentLocale() } returns Locale.US
        mapper = AccountStatusUiMapperImpl(localeProvider)
    }

    @Test
    fun `formatJoinDate returns empty string when createdAt is null`() {
        val result = mapper.formatJoinDate(null)
        assertEquals("", result)
    }

    @Test
    fun `formatJoinDate formats createdAt date correctly using locale`() {
        val createdAt = LocalDateTime.of(2026, 6, 11, 14, 20)
        val result = mapper.formatJoinDate(createdAt)
        assertEquals(true, result.contains("2026"))
        assertEquals(true, result.contains("Jun") || result.contains("June"))
    }
}
