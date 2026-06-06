package es.pedrazamiguez.splittrip.di.domain

import es.pedrazamiguez.splittrip.domain.repository.CurrencyRepository
import es.pedrazamiguez.splittrip.domain.usecase.currency.GetExchangeRateUseCase
import es.pedrazamiguez.splittrip.domain.usecase.currency.GetSupportedCurrenciesUseCase
import es.pedrazamiguez.splittrip.domain.usecase.currency.WarmCurrencyCacheUseCase
import org.koin.dsl.module

val currenciesDomainModule = module {
    factory<GetSupportedCurrenciesUseCase> {
        GetSupportedCurrenciesUseCase(currencyRepository = get<CurrencyRepository>())
    }
    factory<GetExchangeRateUseCase> {
        GetExchangeRateUseCase(currencyRepository = get<CurrencyRepository>())
    }
    factory<WarmCurrencyCacheUseCase> {
        WarmCurrencyCacheUseCase(currencyRepository = get<CurrencyRepository>())
    }
}
