package es.pedrazamiguez.splittrip.domain.usecase.balance

import es.pedrazamiguez.splittrip.domain.model.Balance
import es.pedrazamiguez.splittrip.domain.usecase.UseCase

interface GetBalancesUseCase : UseCase {
    suspend operator fun invoke(): Result<List<Balance>>
}
