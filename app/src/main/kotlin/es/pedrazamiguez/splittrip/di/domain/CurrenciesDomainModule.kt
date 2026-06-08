package es.pedrazamiguez.splittrip.di.domain

import es.pedrazamiguez.splittrip.domain.repository.CurrencyRepository
import es.pedrazamiguez.splittrip.domain.usecase.currency.GetExchangeRateUseCase
import es.pedrazamiguez.splittrip.domain.usecase.currency.GetSupportedCurrenciesUseCase
import es.pedrazamiguez.splittrip.domain.usecase.currency.WarmCurrencyCacheUseCase
import es.pedrazamiguez.splittrip.domain.usecase.currency.impl.GetExchangeRateUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.currency.impl.GetSupportedCurrenciesUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.currency.impl.WarmCurrencyCacheUseCaseImpl
import org.koin.dsl.module

val currenciesDomainModule = module {
    factory<GetSupportedCurrenciesUseCase> {
        GetSupportedCurrenciesUseCaseImpl(currencyRepository = get<CurrencyRepository>())
    }
    factory<GetExchangeRateUseCase> {
        GetExchangeRateUseCaseImpl(currencyRepository = get<CurrencyRepository>())
    }
    factory<WarmCurrencyCacheUseCase> {
        WarmCurrencyCacheUseCaseImpl(currencyRepository = get<CurrencyRepository>())
    }
}
