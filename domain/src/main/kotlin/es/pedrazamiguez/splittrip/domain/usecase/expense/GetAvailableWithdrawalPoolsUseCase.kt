package es.pedrazamiguez.splittrip.domain.usecase.expense

import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.domain.model.WithdrawalPoolOption
import es.pedrazamiguez.splittrip.domain.usecase.UseCase

interface GetAvailableWithdrawalPoolsUseCase : UseCase {
    suspend operator fun invoke(
        groupId: String,
        currency: String,
        payerType: PayerType,
        payerId: String? = null,
        subunitIds: List<String> = emptyList()
    ): List<WithdrawalPoolOption>
}
