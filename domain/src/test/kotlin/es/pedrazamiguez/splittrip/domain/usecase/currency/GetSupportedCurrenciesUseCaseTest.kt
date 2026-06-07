package es.pedrazamiguez.splittrip.domain.usecase.currency

import es.pedrazamiguez.splittrip.domain.model.Currency
import es.pedrazamiguez.splittrip.domain.repository.CurrencyRepository
import es.pedrazamiguez.splittrip.domain.usecase.currency.impl.GetSupportedCurrenciesUseCaseImpl
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GetSupportedCurrenciesUseCaseTest {

    private val currencyRepository = mockk<CurrencyRepository>()
    private val getSupportedCurrenciesUseCase = GetSupportedCurrenciesUseCaseImpl(currencyRepository)

    @Test
    fun `returns currencies sorted with common currencies first`() = runTest {
        // Given currencies in random order
        val currencies = listOf(
            Currency("THB", "฿", "Thai Baht", 2),
            Currency("EUR", "€", "Euro", 2),
            Currency("JPY", "¥", "Japanese Yen", 0),
            Currency("USD", "$", "US Dollar", 2),
            Currency("ARS", "$", "Argentine Peso", 2)
        )

        coEvery { currencyRepository.getCurrencies(any()) } returns currencies

        // When
        val result = getSupportedCurrenciesUseCase()

        // Then
        assertTrue(result.isSuccess)
        val sortedCurrencies = result.getOrThrow()

        // EUR should be first, then USD, then JPY (based on PREFERRED_CURRENCY_ORDER)
        assertEquals("EUR", sortedCurrencies[0].code)
        assertEquals("USD", sortedCurrencies[1].code)
        assertEquals("JPY", sortedCurrencies[2].code)

        // Non-preferred currencies should be sorted alphabetically at the end
        assertEquals("ARS", sortedCurrencies[3].code)
        assertEquals("THB", sortedCurrencies[4].code)
    }

    @Test
    fun `returns failure on repository exception`() = runTest {
        val exception = RuntimeException("Network error")
        coEvery { currencyRepository.getCurrencies(any()) } throws exception

        val result = getSupportedCurrenciesUseCase()

        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }

    @Test
    fun `respects forceRefresh parameter`() = runTest {
        val currencies = listOf(Currency("EUR", "€", "Euro", 2))
        coEvery { currencyRepository.getCurrencies(true) } returns currencies

        val result = getSupportedCurrenciesUseCase(forceRefresh = true)

        assertTrue(result.isSuccess)
    }

    @Test
    fun `returns empty list when repository returns empty`() = runTest {
        coEvery { currencyRepository.getCurrencies(any()) } returns emptyList()

        val result = getSupportedCurrenciesUseCase()

        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow().isEmpty())
    }

    @Test
    fun `maintains order for all preferred currencies`() = runTest {
        // Test with multiple preferred currencies to ensure order is maintained
        val currencies = listOf(
            Currency("ZAR", "R", "South African Rand", 2),
            Currency("GBP", "£", "British Pound", 2),
            Currency("CHF", "Fr", "Swiss Franc", 2),
            Currency("EUR", "€", "Euro", 2)
        )

        coEvery { currencyRepository.getCurrencies(any()) } returns currencies

        val result = getSupportedCurrenciesUseCase()

        assertTrue(result.isSuccess)
        val sortedCurrencies = result.getOrThrow()

        // Order should match PREFERRED_CURRENCY_ORDER: EUR, GBP, CHF, ZAR
        assertEquals("EUR", sortedCurrencies[0].code)
        assertEquals("GBP", sortedCurrencies[1].code)
        assertEquals("CHF", sortedCurrencies[2].code)
        assertEquals("ZAR", sortedCurrencies[3].code)
    }
}
