package es.pedrazamiguez.splittrip.domain.usecase.expense

import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.domain.model.Expense
import es.pedrazamiguez.splittrip.domain.usecase.expense.factory.PersistExpenseStrategyFactory

class AddExpenseUseCase(
    private val strategyFactory: PersistExpenseStrategyFactory
) {

    suspend operator fun invoke(
        groupId: String?,
        expense: Expense,
        pairedContributionScope: PayerType = PayerType.USER,
        pairedSubunitId: String? = null,
        preferredWithdrawalScope: PayerType? = null,
        preferredWithdrawalOwnerId: String? = null
    ): Result<Unit> {
        if (groupId.isNullOrBlank()) {
            return Result.failure(IllegalArgumentException("Group ID cannot be null or blank"))
        }
        val strategy = strategyFactory.create(isUpdate = false)
        return strategy.persist(
            groupId = groupId,
            expense = expense,
            pairedContributionScope = pairedContributionScope,
            pairedSubunitId = pairedSubunitId,
            preferredWithdrawalScope = preferredWithdrawalScope,
            preferredWithdrawalOwnerId = preferredWithdrawalOwnerId
        )
    }
}
