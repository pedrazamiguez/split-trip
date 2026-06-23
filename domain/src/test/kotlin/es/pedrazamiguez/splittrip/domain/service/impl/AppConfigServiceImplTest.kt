package es.pedrazamiguez.splittrip.domain.service.impl

import es.pedrazamiguez.splittrip.domain.repository.AppConfigRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AppConfigServiceImplTest {

    @Test
    fun `exposes flows from repository correctly`() {
        // Given
        val repository = mockk<AppConfigRepository>()
        val defaultCurrencyFlow = MutableStateFlow("EUR")
        val debounceFlow = MutableStateFlow(300L)
        every { repository.defaultCurrencyCode } returns defaultCurrencyFlow
        every { repository.balanceComputationDebounceMs } returns debounceFlow

        // When
        val service = AppConfigServiceImpl(repository)

        // Then
        assertEquals(defaultCurrencyFlow, service.defaultCurrencyCode)
        assertEquals(debounceFlow, service.balanceComputationDebounceMs)
        assertEquals("EUR", service.defaultCurrencyCode.value)
        assertEquals(300L, service.balanceComputationDebounceMs.value)
    }
}
