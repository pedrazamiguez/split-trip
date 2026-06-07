package es.pedrazamiguez.splittrip.domain.usecase.currency

import es.pedrazamiguez.splittrip.domain.usecase.UseCase

interface WarmCurrencyCacheUseCase : UseCase {
    suspend operator fun invoke()
}
