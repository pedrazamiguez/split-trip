package es.pedrazamiguez.splittrip.domain.usecase.currency.impl

import es.pedrazamiguez.splittrip.domain.repository.CurrencyRepository
import es.pedrazamiguez.splittrip.domain.usecase.currency.WarmCurrencyCacheUseCase

class WarmCurrencyCacheUseCaseImpl(private val currencyRepository: CurrencyRepository) : WarmCurrencyCacheUseCase {

    /**
     * USD base code — the only base currency supported by the
     * OpenExchangeRates Free Tier.
     */
    companion object {
        private const val USD_BASE = "USD"
    }

    override suspend operator fun invoke() {
        runCatching { currencyRepository.getCurrencies(forceRefresh = false) }
        runCatching { currencyRepository.getExchangeRates(USD_BASE) }
    }
}
