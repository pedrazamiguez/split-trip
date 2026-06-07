package es.pedrazamiguez.splittrip.domain.usecase.currency

import es.pedrazamiguez.splittrip.domain.model.Currency
import es.pedrazamiguez.splittrip.domain.model.ExchangeRate
import es.pedrazamiguez.splittrip.domain.model.ExchangeRates
import es.pedrazamiguez.splittrip.domain.repository.CurrencyRepository
import es.pedrazamiguez.splittrip.domain.result.ExchangeRateResult
import es.pedrazamiguez.splittrip.domain.usecase.currency.impl.WarmCurrencyCacheUseCaseImpl
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.math.BigDecimal
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("WarmCurrencyCacheUseCase")
class WarmCurrencyCacheUseCaseTest {

    private lateinit var currencyRepository: CurrencyRepository
    private lateinit var useCase: WarmCurrencyCacheUseCase

    private val usd = Currency("USD", "$", "US Dollar", 2)
    private val eur = Currency("EUR", "€", "Euro", 2)

    private val currencies = listOf(usd, eur)
    private val exchangeRateResult = ExchangeRateResult.Fresh(
        ExchangeRates(
            baseCurrency = usd,
            exchangeRates = listOf(ExchangeRate(eur, BigDecimal("0.9"))),
            lastUpdated = Instant.now()
        )
    )

    @BeforeEach
    fun setUp() {
        currencyRepository = mockk()
        useCase = WarmCurrencyCacheUseCaseImpl(currencyRepository)
    }

    @Nested
    @DisplayName("Success scenarios")
    inner class SuccessScenarios {

        @Test
        fun `calls getCurrencies with forceRefresh false`() = runTest {
            // Given
            coEvery { currencyRepository.getCurrencies(false) } returns currencies
            coEvery { currencyRepository.getExchangeRates("USD") } returns exchangeRateResult

            // When
            useCase()

            // Then
            coVerify(exactly = 1) { currencyRepository.getCurrencies(forceRefresh = false) }
        }

        @Test
        fun `calls getExchangeRates with USD base`() = runTest {
            // Given
            coEvery { currencyRepository.getCurrencies(false) } returns currencies
            coEvery { currencyRepository.getExchangeRates("USD") } returns exchangeRateResult

            // When
            useCase()

            // Then
            coVerify(exactly = 1) { currencyRepository.getExchangeRates("USD") }
        }

        @Test
        fun `calls both repository methods when both succeed`() = runTest {
            // Given
            coEvery { currencyRepository.getCurrencies(false) } returns currencies
            coEvery { currencyRepository.getExchangeRates("USD") } returns exchangeRateResult

            // When
            useCase()

            // Then
            coVerify(exactly = 1) { currencyRepository.getCurrencies(forceRefresh = false) }
            coVerify(exactly = 1) { currencyRepository.getExchangeRates("USD") }
        }
    }

    @Nested
    @DisplayName("Failure isolation")
    inner class FailureIsolation {

        @Test
        fun `getCurrencies failure does not prevent getExchangeRates call`() = runTest {
            // Given
            coEvery {
                currencyRepository.getCurrencies(false)
            } throws RuntimeException("Network error")
            coEvery { currencyRepository.getExchangeRates("USD") } returns exchangeRateResult

            // When — should not throw
            useCase()

            // Then — exchange rates still called despite currency list failure
            coVerify(exactly = 1) { currencyRepository.getCurrencies(forceRefresh = false) }
            coVerify(exactly = 1) { currencyRepository.getExchangeRates("USD") }
        }

        @Test
        fun `getExchangeRates failure does not affect getCurrencies call`() = runTest {
            // Given
            coEvery { currencyRepository.getCurrencies(false) } returns currencies
            coEvery {
                currencyRepository.getExchangeRates("USD")
            } throws RuntimeException("Network error")

            // When — should not throw
            useCase()

            // Then — both were called, currency list succeeded
            coVerify(exactly = 1) { currencyRepository.getCurrencies(forceRefresh = false) }
            coVerify(exactly = 1) { currencyRepository.getExchangeRates("USD") }
        }

        @Test
        fun `both failing does not throw`() = runTest {
            // Given
            coEvery {
                currencyRepository.getCurrencies(false)
            } throws RuntimeException("Currency API down")
            coEvery {
                currencyRepository.getExchangeRates("USD")
            } throws RuntimeException("Rate API down")

            // When — should not throw even when both fail
            useCase()

            // Then — both were attempted
            coVerify(exactly = 1) { currencyRepository.getCurrencies(forceRefresh = false) }
            coVerify(exactly = 1) { currencyRepository.getExchangeRates("USD") }
        }
    }
}
