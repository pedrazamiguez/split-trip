package es.pedrazamiguez.splittrip.domain.usecase.setting

import es.pedrazamiguez.splittrip.domain.usecase.UseCase

interface SetGroupLastUsedCurrencyUseCase : UseCase {
    suspend operator fun invoke(groupId: String, currencyCode: String)
}
