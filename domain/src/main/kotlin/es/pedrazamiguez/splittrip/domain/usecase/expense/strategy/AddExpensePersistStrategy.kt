package es.pedrazamiguez.splittrip.domain.usecase.expense.strategy

import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.domain.model.Expense
import es.pedrazamiguez.splittrip.domain.repository.CashWithdrawalRepository
import es.pedrazamiguez.splittrip.domain.repository.ContributionRepository
import es.pedrazamiguez.splittrip.domain.repository.ExpenseRepository
import es.pedrazamiguez.splittrip.domain.service.AddOnCalculationService
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.service.ExchangeRateCalculationService
import es.pedrazamiguez.splittrip.domain.service.ExpenseCalculatorService
import es.pedrazamiguez.splittrip.domain.service.GroupMembershipService
import java.util.UUID

class AddExpensePersistStrategy(
    expenseRepository: ExpenseRepository,
    cashWithdrawalRepository: CashWithdrawalRepository,
    expenseCalculatorService: ExpenseCalculatorService,
    exchangeRateCalculationService: ExchangeRateCalculationService,
    groupMembershipService: GroupMembershipService,
    contributionRepository: ContributionRepository,
    authenticationService: AuthenticationService,
    addOnCalculationService: AddOnCalculationService
) : BasePersistExpenseStrategy(
    expenseRepository,
    cashWithdrawalRepository,
    expenseCalculatorService,
    exchangeRateCalculationService,
    groupMembershipService,
    contributionRepository,
    authenticationService,
    addOnCalculationService
) {

    override suspend fun persist(
        groupId: String,
        expense: Expense,
        pairedContributionScope: PayerType,
        pairedSubunitId: String?,
        preferredWithdrawalScope: PayerType?,
        preferredWithdrawalOwnerId: String?
    ): Result<Unit> = runCatching {
        require(expense.sourceAmount > 0) { "Expense amount must be greater than zero" }
        require(expense.title.isNotBlank()) { "Expense title cannot be empty" }

        groupMembershipService.requireMembership(groupId)

        val expenseWithId = if (expense.id.isBlank()) {
            expense.copy(id = UUID.randomUUID().toString())
        } else {
            expense
        }

        val expenseToSave = saveExpense(
            groupId = groupId,
            expense = expenseWithId,
            preferredWithdrawalScope = preferredWithdrawalScope,
            preferredWithdrawalOwnerId = preferredWithdrawalOwnerId
        )

        if (hasActiveUserPayer(expenseToSave)) {
            try {
                createPairedContributions(
                    groupId,
                    expenseToSave,
                    pairedContributionScope,
                    pairedSubunitId
                )
            } catch (exception: Exception) {
                runCatching {
                    expenseRepository.deleteExpense(groupId, expenseToSave.id)
                }.exceptionOrNull()?.let { rollbackException ->
                    exception.addSuppressed(rollbackException)
                }
                throw exception
            }
        }
    }

    private suspend fun saveExpense(
        groupId: String,
        expense: Expense,
        preferredWithdrawalScope: PayerType?,
        preferredWithdrawalOwnerId: String?
    ): Expense {
        if (!hasActiveCashPayment(expense)) {
            expenseRepository.addExpense(groupId, expense)
            return expense
        }
        val fifoResult = computeFifoResult(
            groupId = groupId,
            expense = expense,
            preferredWithdrawalScope = preferredWithdrawalScope,
            preferredWithdrawalOwnerId = preferredWithdrawalOwnerId
        )
        val transactionCommitted = expenseRepository.addCashExpense(
            groupId,
            fifoResult.expense,
            fifoResult.expectedRemainingAmounts
        )
        if (transactionCommitted) {
            cashWithdrawalRepository.updateRemainingAmounts(groupId, fifoResult.updatedWithdrawals)
        }
        return fifoResult.expense
    }
}
