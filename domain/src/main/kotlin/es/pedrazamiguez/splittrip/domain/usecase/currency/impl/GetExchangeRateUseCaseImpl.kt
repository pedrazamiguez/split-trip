package es.pedrazamiguez.splittrip.domain.usecase.currency.impl

import es.pedrazamiguez.splittrip.domain.model.ExchangeRate
import es.pedrazamiguez.splittrip.domain.repository.CurrencyRepository
import es.pedrazamiguez.splittrip.domain.result.ExchangeRateResult
import es.pedrazamiguez.splittrip.domain.result.ExchangeRateWithStaleness
import es.pedrazamiguez.splittrip.domain.usecase.currency.GetExchangeRateUseCase
import java.math.BigDecimal
import java.math.MathContext
import java.time.Instant

class GetExchangeRateUseCaseImpl(private val currencyRepository: CurrencyRepository) : GetExchangeRateUseCase {

    private data class ResolvedExchangeRates(
        val rates: List<ExchangeRate>,
        val isStale: Boolean,
        val lastUpdated: Instant
    )

    /**
     * Calculates the cross-rate: 1 [baseCurrencyCode] = X [targetCurrencyCode].
     * Uses USD as pivot to support OpenExchangeRates Free Tier.
     *
     * Returns [ExchangeRateWithStaleness] containing the rate plus a flag indicating
     * whether the underlying data was served from an expired cache (i.e., the remote
     * API could not be reached to refresh it).
     */
    override suspend operator fun invoke(
        baseCurrencyCode: String,
        targetCurrencyCode: String
    ): ExchangeRateWithStaleness? {
        // Free tier only allows fetching USD base.
        val result = currencyRepository.getExchangeRates("USD")

        val (ratesList, isStale, lastUpdated) = when (result) {
            is ExchangeRateResult.Fresh -> ResolvedExchangeRates(
                rates = result.exchangeRates.exchangeRates,
                isStale = false,
                lastUpdated = result.exchangeRates.lastUpdated
            )
            is ExchangeRateResult.Stale -> ResolvedExchangeRates(
                rates = result.exchangeRates.exchangeRates,
                isStale = true,
                lastUpdated = result.exchangeRates.lastUpdated
            )
            ExchangeRateResult.Empty -> return null
        }

        // Convert list to map for efficient lookup
        val ratesMap = ratesList.associate { it.currency.code to it.rate }

        // Helper to get rate safely (USD itself is 1.0)
        fun getUsdRate(code: String): BigDecimal? = if (code == "USD") BigDecimal.ONE else ratesMap[code]

        val usdToBase = getUsdRate(baseCurrencyCode)
            ?.takeIf { it.compareTo(BigDecimal.ZERO) != 0 }
            ?: return null
        val usdToTarget = getUsdRate(targetCurrencyCode) ?: return null

        // Triangulation: TargetRate / BaseRate
        // Example: 1 USD = 0.9 EUR; 1 USD = 30 THB
        // 1 EUR = 30 / 0.9 = 33.33 THB
        val rate = usdToTarget.divide(usdToBase, MathContext.DECIMAL64)

        return ExchangeRateWithStaleness(
            rate = rate,
            isStale = isStale,
            lastUpdated = lastUpdated
        )
    }
}
