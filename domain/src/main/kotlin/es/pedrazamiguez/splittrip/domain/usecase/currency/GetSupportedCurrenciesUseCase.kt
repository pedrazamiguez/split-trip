package es.pedrazamiguez.splittrip.domain.usecase.currency

import es.pedrazamiguez.splittrip.domain.model.Currency
import es.pedrazamiguez.splittrip.domain.usecase.UseCase

interface GetSupportedCurrenciesUseCase : UseCase {
    suspend operator fun invoke(forceRefresh: Boolean = false): Result<List<Currency>>
}
