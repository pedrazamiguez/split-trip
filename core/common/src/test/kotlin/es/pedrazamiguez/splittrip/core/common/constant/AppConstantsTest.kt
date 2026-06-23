package es.pedrazamiguez.splittrip.core.common.constant

import es.pedrazamiguez.splittrip.core.common.provider.RemoteConfigProvider
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AppConstantsTest {

    @BeforeEach
    @AfterEach
    fun resetRemoteConfigProvider() {
        AppConstants.remoteConfigProvider = null
        clearAllMocks()
    }

    @Test
    fun `DEFAULT_CURRENCY_CODE returns fallback value when remoteConfigProvider is null`() {
        assertEquals("EUR", AppConstants.DEFAULT_CURRENCY_CODE)
    }

    @Test
    fun `BALANCE_COMPUTATION_DEBOUNCE_MS returns fallback value when remoteConfigProvider is null`() {
        assertEquals(300L, AppConstants.BALANCE_COMPUTATION_DEBOUNCE_MS)
    }

    @Test
    fun `DEFAULT_CURRENCY_CODE returns value from remoteConfigProvider when set`() {
        val mockProvider = mockk<RemoteConfigProvider>()
        every { mockProvider.getString("default_currency_code") } returns "USD"
        AppConstants.remoteConfigProvider = mockProvider

        assertEquals("USD", AppConstants.DEFAULT_CURRENCY_CODE)
    }

    @Test
    fun `BALANCE_COMPUTATION_DEBOUNCE_MS returns value from remoteConfigProvider when set`() {
        val mockProvider = mockk<RemoteConfigProvider>()
        every { mockProvider.getLong("balance_computation_debounce_ms") } returns 500L
        AppConstants.remoteConfigProvider = mockProvider

        assertEquals(500L, AppConstants.BALANCE_COMPUTATION_DEBOUNCE_MS)
    }
}
