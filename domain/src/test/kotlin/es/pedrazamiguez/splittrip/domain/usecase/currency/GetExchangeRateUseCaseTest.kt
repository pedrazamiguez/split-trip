package es.pedrazamiguez.splittrip.domain.usecase.currency

import es.pedrazamiguez.splittrip.domain.model.Currency
import es.pedrazamiguez.splittrip.domain.model.ExchangeRate
import es.pedrazamiguez.splittrip.domain.model.ExchangeRates
import es.pedrazamiguez.splittrip.domain.repository.CurrencyRepository
import es.pedrazamiguez.splittrip.domain.result.ExchangeRateResult
import es.pedrazamiguez.splittrip.domain.usecase.currency.impl.GetExchangeRateUseCaseImpl
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.math.BigDecimal
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class GetExchangeRateUseCaseTest {

    private lateinit var currencyRepository: CurrencyRepository
    private lateinit var useCase: GetExchangeRateUseCase

    private val usd = Currency("USD", "$", "US Dollar", 2)
    private val eur = Currency("EUR", "€", "Euro", 2)
    private val thb = Currency("THB", "฿", "Thai Baht", 2)
    private val jpy = Currency("JPY", "¥", "Japanese Yen", 0)

    @BeforeEach
    fun setUp() {
        currencyRepository = mockk()
        useCase = GetExchangeRateUseCaseImpl(currencyRepository)
    }

    @Nested
    inner class TriangulationCalculation {

        @Test
        fun `calculates cross-rate via USD triangulation - EUR to THB`() = runTest {
            // Given: USD rates where 1 USD = 0.9 EUR and 1 USD = 35 THB
            // Expected: 1 EUR = 35 / 0.9 = 38.888... THB
            val rates = ExchangeRates(
                baseCurrency = usd,
                exchangeRates = listOf(
                    ExchangeRate(eur, BigDecimal("0.9")),
                    ExchangeRate(thb, BigDecimal("35"))
                ),
                lastUpdated = Instant.now()
            )
            coEvery { currencyRepository.getExchangeRates("USD") } returns ExchangeRateResult.Fresh(rates)

            // When
            val result = useCase(baseCurrencyCode = "EUR", targetCurrencyCode = "THB")

            // Then
            // 35 / 0.9 ≈ 38.888...
            assertNotNull(result)
            assertTrue(result!!.rate.subtract(BigDecimal("38.888")).abs() < BigDecimal("0.01"))
            assertFalse(result.isStale)
            coVerify { currencyRepository.getExchangeRates("USD") }
        }

        @Test
        fun `calculates cross-rate via USD triangulation - THB to EUR`() = runTest {
            // Given: USD rates where 1 USD = 0.9 EUR and 1 USD = 35 THB
            // Expected: 1 THB = 0.9 / 35 = 0.0257... EUR
            val rates = ExchangeRates(
                baseCurrency = usd,
                exchangeRates = listOf(
                    ExchangeRate(eur, BigDecimal("0.9")),
                    ExchangeRate(thb, BigDecimal("35"))
                ),
                lastUpdated = Instant.now()
            )
            coEvery { currencyRepository.getExchangeRates("USD") } returns ExchangeRateResult.Fresh(rates)

            // When
            val result = useCase(baseCurrencyCode = "THB", targetCurrencyCode = "EUR")

            // Then
            // 0.9 / 35 ≈ 0.0257142...
            assertNotNull(result)
            assertTrue(result!!.rate.subtract(BigDecimal("0.0257")).abs() < BigDecimal("0.001"))
        }

        @Test
        fun `calculates rate when base is USD`() = runTest {
            // Given: 1 USD = 35 THB
            // Expected: 1 USD = 35 THB (direct)
            val rates = ExchangeRates(
                baseCurrency = usd,
                exchangeRates = listOf(
                    ExchangeRate(thb, BigDecimal("35"))
                ),
                lastUpdated = Instant.now()
            )
            coEvery { currencyRepository.getExchangeRates("USD") } returns ExchangeRateResult.Fresh(rates)

            // When
            val result = useCase(baseCurrencyCode = "USD", targetCurrencyCode = "THB")

            // Then
            // 35 / 1 = 35
            assertEquals(BigDecimal("35"), result!!.rate)
        }

        @Test
        fun `calculates rate when target is USD`() = runTest {
            // Given: 1 USD = 0.9 EUR
            // Expected: 1 EUR = 1 / 0.9 = 1.111... USD
            val rates = ExchangeRates(
                baseCurrency = usd,
                exchangeRates = listOf(
                    ExchangeRate(eur, BigDecimal("0.9"))
                ),
                lastUpdated = Instant.now()
            )
            coEvery { currencyRepository.getExchangeRates("USD") } returns ExchangeRateResult.Fresh(rates)

            // When
            val result = useCase(baseCurrencyCode = "EUR", targetCurrencyCode = "USD")

            // Then
            // 1 / 0.9 ≈ 1.111...
            assertNotNull(result)
            assertTrue(result!!.rate.subtract(BigDecimal("1.111")).abs() < BigDecimal("0.01"))
        }

        @Test
        fun `returns 1 when base and target are both USD`() = runTest {
            // Given
            val rates = ExchangeRates(
                baseCurrency = usd,
                exchangeRates = listOf(
                    ExchangeRate(eur, BigDecimal("0.9"))
                ),
                lastUpdated = Instant.now()
            )
            coEvery { currencyRepository.getExchangeRates("USD") } returns ExchangeRateResult.Fresh(rates)

            // When
            val result = useCase(baseCurrencyCode = "USD", targetCurrencyCode = "USD")

            // Then
            assertEquals(BigDecimal.ONE, result!!.rate)
        }
    }

    @Nested
    inner class StaleRates {

        @Test
        fun `uses stale rates when fresh not available`() = runTest {
            // Given stale rates
            val rates = ExchangeRates(
                baseCurrency = usd,
                exchangeRates = listOf(
                    ExchangeRate(eur, BigDecimal("0.85")),
                    ExchangeRate(thb, BigDecimal("33"))
                ),
                lastUpdated = Instant.now().minusSeconds(86400) // 1 day old
            )
            coEvery { currencyRepository.getExchangeRates("USD") } returns ExchangeRateResult.Stale(rates)

            // When
            val result = useCase(baseCurrencyCode = "EUR", targetCurrencyCode = "THB")

            // Then
            // 33 / 0.85 ≈ 38.823...
            assertNotNull(result)
            assertTrue(result!!.rate.subtract(BigDecimal("38.823")).abs() < BigDecimal("0.01"))
            assertTrue(result.isStale)
        }
    }

    @Nested
    inner class ErrorScenarios {

        @Test
        fun `returns null when repository returns empty`() = runTest {
            // Given
            coEvery { currencyRepository.getExchangeRates("USD") } returns ExchangeRateResult.Empty

            // When
            val result = useCase(baseCurrencyCode = "EUR", targetCurrencyCode = "THB")

            // Then
            assertNull(result)
        }

        @Test
        fun `returns null when base currency not found`() = runTest {
            // Given: rates don't include GBP
            val rates = ExchangeRates(
                baseCurrency = usd,
                exchangeRates = listOf(
                    ExchangeRate(eur, BigDecimal("0.9"))
                ),
                lastUpdated = Instant.now()
            )
            coEvery { currencyRepository.getExchangeRates("USD") } returns ExchangeRateResult.Fresh(rates)

            // When
            val result = useCase(baseCurrencyCode = "GBP", targetCurrencyCode = "EUR")

            // Then
            assertNull(result)
        }

        @Test
        fun `returns null when target currency not found`() = runTest {
            // Given: rates don't include JPY
            val rates = ExchangeRates(
                baseCurrency = usd,
                exchangeRates = listOf(
                    ExchangeRate(eur, BigDecimal("0.9"))
                ),
                lastUpdated = Instant.now()
            )
            coEvery { currencyRepository.getExchangeRates("USD") } returns ExchangeRateResult.Fresh(rates)

            // When
            val result = useCase(baseCurrencyCode = "EUR", targetCurrencyCode = "JPY")

            // Then
            assertNull(result)
        }

        @Test
        fun `returns null when base rate is zero`() = runTest {
            // Given: edge case where rate is 0 (should not happen in practice)
            val rates = ExchangeRates(
                baseCurrency = usd,
                exchangeRates = listOf(
                    ExchangeRate(eur, BigDecimal.ZERO),
                    ExchangeRate(thb, BigDecimal("35"))
                ),
                lastUpdated = Instant.now()
            )
            coEvery { currencyRepository.getExchangeRates("USD") } returns ExchangeRateResult.Fresh(rates)

            // When
            val result = useCase(baseCurrencyCode = "EUR", targetCurrencyCode = "THB")

            // Then
            assertNull(result)
        }
    }

    @Nested
    inner class AlwaysUsesUsdBase {

        @Test
        fun `always fetches USD rates regardless of base currency`() = runTest {
            // Given
            val rates = ExchangeRates(
                baseCurrency = usd,
                exchangeRates = listOf(
                    ExchangeRate(eur, BigDecimal("0.9")),
                    ExchangeRate(jpy, BigDecimal("150"))
                ),
                lastUpdated = Instant.now()
            )
            coEvery { currencyRepository.getExchangeRates("USD") } returns ExchangeRateResult.Fresh(rates)

            // When - asking for EUR to JPY
            useCase(baseCurrencyCode = "EUR", targetCurrencyCode = "JPY")

            // Then - should still fetch USD rates (free tier constraint)
            coVerify(exactly = 1) { currencyRepository.getExchangeRates("USD") }
        }
    }
}
