package es.pedrazamiguez.splittrip.domain.usecase.expense.impl

import es.pedrazamiguez.splittrip.domain.repository.CashWithdrawalRepository
import es.pedrazamiguez.splittrip.domain.repository.ContributionRepository
import es.pedrazamiguez.splittrip.domain.repository.ExpenseRepository
import es.pedrazamiguez.splittrip.domain.service.GroupMembershipService
import es.pedrazamiguez.splittrip.domain.usecase.expense.DeleteExpenseUseCase

class DeleteExpenseUseCaseImpl(
    private val expenseRepository: ExpenseRepository,
    private val cashWithdrawalRepository: CashWithdrawalRepository,
    private val groupMembershipService: GroupMembershipService,
    private val contributionRepository: ContributionRepository
) : DeleteExpenseUseCase {

    /**
     * Deletes an expense by its ID within a group.
     *
     * If the expense was paid in CASH, restores the consumed amounts back
     * to the respective CashWithdrawal records before deleting.
     *
     * If the expense was out-of-pocket (payerType = USER), cascade-deletes
     * the linked paired contribution. This is safe to call for GROUP expenses
     * too — [ContributionRepository.deleteByLinkedExpenseId] is a graceful no-op
     * when no linked contribution exists.
     *
     * @param groupId The ID of the group containing the expense.
     * @param expenseId The ID of the expense to delete.
     * @throws NotGroupMemberException if the user is not a member of the group.
     */
    override suspend operator fun invoke(groupId: String, expenseId: String) {
        groupMembershipService.requireMembership(groupId)

        // Fetch the expense to check for cash tranches before deletion
        val expense = expenseRepository.getExpenseById(expenseId)

        // Refund cash tranches if this was a cash expense
        expense?.cashTranches?.forEach { tranche ->
            cashWithdrawalRepository.refundTranche(
                withdrawalId = tranche.withdrawalId,
                amountToRefund = tranche.amountConsumed
            )
        }

        // Cascade-delete linked paired contribution (no-op if none exists)
        contributionRepository.deleteByLinkedExpenseId(groupId, expenseId)

        expenseRepository.deleteExpense(groupId, expenseId)
    }
}
