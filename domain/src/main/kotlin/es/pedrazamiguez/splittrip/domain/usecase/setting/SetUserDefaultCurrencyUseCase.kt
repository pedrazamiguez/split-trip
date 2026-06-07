package es.pedrazamiguez.splittrip.domain.usecase.setting

import es.pedrazamiguez.splittrip.domain.usecase.UseCase

interface SetUserDefaultCurrencyUseCase : UseCase {
    suspend operator fun invoke(currencyCode: String)
}
