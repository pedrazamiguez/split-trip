package es.pedrazamiguez.splittrip.domain.usecase.expense

import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.domain.model.Expense
import es.pedrazamiguez.splittrip.domain.usecase.UseCase

interface AddExpenseUseCase : UseCase {
    suspend operator fun invoke(
        groupId: String?,
        expense: Expense,
        pairedContributionScope: PayerType = PayerType.USER,
        pairedSubunitId: String? = null,
        preferredWithdrawalScope: PayerType? = null,
        preferredWithdrawalOwnerId: String? = null
    ): Result<Unit>
}
