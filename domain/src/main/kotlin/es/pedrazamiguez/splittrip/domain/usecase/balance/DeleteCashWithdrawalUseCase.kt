package es.pedrazamiguez.splittrip.domain.usecase.balance

import es.pedrazamiguez.splittrip.domain.usecase.UseCase

interface DeleteCashWithdrawalUseCase : UseCase {
    suspend operator fun invoke(groupId: String, withdrawalId: String)
}
