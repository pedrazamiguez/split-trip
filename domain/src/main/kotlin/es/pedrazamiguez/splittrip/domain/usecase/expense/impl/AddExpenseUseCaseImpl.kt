package es.pedrazamiguez.splittrip.domain.usecase.expense.impl

import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.domain.model.Expense
import es.pedrazamiguez.splittrip.domain.usecase.expense.AddExpenseUseCase
import es.pedrazamiguez.splittrip.domain.usecase.expense.factory.PersistExpenseStrategyFactory

class AddExpenseUseCaseImpl(
    private val strategyFactory: PersistExpenseStrategyFactory
) : AddExpenseUseCase {

    override suspend operator fun invoke(
        groupId: String?,
        expense: Expense,
        pairedContributionScope: PayerType,
        pairedSubunitId: String?,
        preferredWithdrawalScope: PayerType?,
        preferredWithdrawalOwnerId: String?
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
