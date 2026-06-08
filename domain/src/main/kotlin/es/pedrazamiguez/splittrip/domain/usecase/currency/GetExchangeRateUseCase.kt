package es.pedrazamiguez.splittrip.domain.usecase.currency

import es.pedrazamiguez.splittrip.domain.result.ExchangeRateWithStaleness
import es.pedrazamiguez.splittrip.domain.usecase.UseCase

interface GetExchangeRateUseCase : UseCase {
    suspend operator fun invoke(
        baseCurrencyCode: String,
        targetCurrencyCode: String
    ): ExchangeRateWithStaleness?
}
