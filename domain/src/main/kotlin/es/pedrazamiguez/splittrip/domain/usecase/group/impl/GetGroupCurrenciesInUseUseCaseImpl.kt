package es.pedrazamiguez.splittrip.domain.usecase.group.impl

import es.pedrazamiguez.splittrip.domain.repository.CashTransferRepository
import es.pedrazamiguez.splittrip.domain.repository.CashWithdrawalRepository
import es.pedrazamiguez.splittrip.domain.repository.ContributionRepository
import es.pedrazamiguez.splittrip.domain.repository.ExpenseRepository
import es.pedrazamiguez.splittrip.domain.usecase.group.GetGroupCurrenciesInUseUseCase
import kotlinx.coroutines.flow.first

class GetGroupCurrenciesInUseUseCaseImpl(
    private val expenseRepository: ExpenseRepository,
    private val contributionRepository: ContributionRepository,
    private val cashWithdrawalRepository: CashWithdrawalRepository,
    private val cashTransferRepository: CashTransferRepository
) : GetGroupCurrenciesInUseUseCase {

    override suspend operator fun invoke(groupId: String): Set<String> {
        val expenses = expenseRepository.getGroupExpensesFlow(groupId).first()
        val contributions = contributionRepository.getGroupContributionsFlow(groupId).first()
        val withdrawals = cashWithdrawalRepository.getGroupWithdrawalsFlow(groupId).first()
        val cashTransfers = cashTransferRepository.observeGroupCashTransfers(groupId).first()

        return buildSet {
            expenses.forEach { expense ->
                addIfNotBlank(expense.sourceCurrency)
                addIfNotBlank(expense.groupCurrency)
                expense.addOns.forEach { addOn -> addIfNotBlank(addOn.currency) }
            }
            contributions.forEach { contribution ->
                addIfNotBlank(contribution.currency)
            }
            withdrawals.forEach { withdrawal ->
                addIfNotBlank(withdrawal.currency)
                withdrawal.addOns.forEach { addOn -> addIfNotBlank(addOn.currency) }
            }
            cashTransfers.forEach { transfer ->
                addIfNotBlank(transfer.currency)
            }
        }
    }

    private fun MutableSet<String>.addIfNotBlank(code: String) {
        if (code.isNotBlank()) {
            add(code)
        }
    }
}
