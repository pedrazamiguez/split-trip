package es.pedrazamiguez.splittrip.domain.usecase.expense.strategy

import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.domain.enums.PaymentMethod
import es.pedrazamiguez.splittrip.domain.model.CashWithdrawal
import es.pedrazamiguez.splittrip.domain.model.Contribution
import es.pedrazamiguez.splittrip.domain.model.Expense
import es.pedrazamiguez.splittrip.domain.repository.CashWithdrawalRepository
import es.pedrazamiguez.splittrip.domain.repository.ContributionRepository
import es.pedrazamiguez.splittrip.domain.repository.ExpenseRepository
import es.pedrazamiguez.splittrip.domain.service.AddOnCalculationService
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.service.ExchangeRateCalculationService
import es.pedrazamiguez.splittrip.domain.service.ExpenseCalculatorService
import es.pedrazamiguez.splittrip.domain.service.GroupMembershipService

class UpdateExpensePersistStrategy(
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
        validateInputs(expense)
        groupMembershipService.requireMembership(groupId)

        val originalState = getOriginalState(groupId, expense.id)

        // Refund original tranches locally (updates Room DB)
        originalState.expense.cashTranches.forEach { tranche ->
            cashWithdrawalRepository.refundTranche(tranche.withdrawalId, tranche.amountConsumed)
        }

        try {
            val savedExpense = performPersistence(
                groupId = groupId,
                expense = expense,
                originalWithdrawals = originalState.withdrawals,
                preferredWithdrawalScope = preferredWithdrawalScope,
                preferredWithdrawalOwnerId = preferredWithdrawalOwnerId
            )

            // Update paired contribution
            contributionRepository.deleteByLinkedExpenseId(groupId, expense.id)
            if (savedExpense.payerType == PayerType.USER) {
                createPairedContribution(
                    groupId,
                    savedExpense,
                    pairedContributionScope,
                    pairedSubunitId
                )
            }
        } catch (exception: Exception) {
            rollback(
                groupId = groupId,
                originalState = originalState,
                exception = exception
            )
            throw exception
        }
    }

    private fun validateInputs(expense: Expense) {
        require(expense.id.isNotBlank()) { "Expense ID cannot be blank for update strategy" }
        require(expense.sourceAmount > 0) { "Expense amount must be greater than zero" }
        require(expense.title.isNotBlank()) { "Expense title cannot be empty" }
    }

    private suspend fun getOriginalState(groupId: String, expenseId: String): OriginalExpenseState {
        val originalExpense = expenseRepository.getExpenseById(expenseId)
            ?: throw IllegalArgumentException("Expense not found with ID: $expenseId")

        val originalContribution = contributionRepository.findByLinkedExpenseId(groupId, expenseId)

        val originalTranches = originalExpense.cashTranches
        val originalWithdrawals = originalTranches.mapNotNull { tranche ->
            cashWithdrawalRepository.getWithdrawalById(tranche.withdrawalId)
        }

        return OriginalExpenseState(originalExpense, originalContribution, originalWithdrawals)
    }

    private suspend fun performPersistence(
        groupId: String,
        expense: Expense,
        originalWithdrawals: List<CashWithdrawal>,
        preferredWithdrawalScope: PayerType?,
        preferredWithdrawalOwnerId: String?
    ): Expense {
        return if (expense.paymentMethod == PaymentMethod.CASH) {
            val fifoResult = computeCashFifoResult(
                groupId,
                expense,
                preferredWithdrawalScope,
                preferredWithdrawalOwnerId
            )

            val transactionCommitted = expenseRepository.addCashExpense(
                groupId,
                fifoResult.expense,
                fifoResult.expectedRemainingAmounts
            )

            if (transactionCommitted) {
                cashWithdrawalRepository.updateRemainingAmounts(groupId, fifoResult.updatedWithdrawals)
            } else {
                // Offline fallback: do not apply new deductions, and restore original withdrawals
                if (originalWithdrawals.isNotEmpty()) {
                    cashWithdrawalRepository.updateRemainingAmounts(groupId, originalWithdrawals)
                }
            }

            fifoResult.expense
        } else {
            expenseRepository.addExpense(groupId, expense)
            expense
        }
    }

    private suspend fun rollback(
        groupId: String,
        originalState: OriginalExpenseState,
        exception: Exception
    ) {
        if (originalState.withdrawals.isNotEmpty()) {
            runCatching {
                cashWithdrawalRepository.updateRemainingAmounts(groupId, originalState.withdrawals)
            }.exceptionOrNull()?.let { exception.addSuppressed(it) }
        }

        runCatching {
            expenseRepository.addExpense(groupId, originalState.expense)
        }.exceptionOrNull()?.let { exception.addSuppressed(it) }

        if (originalState.contribution != null) {
            runCatching {
                contributionRepository.addContribution(groupId, originalState.contribution)
            }.exceptionOrNull()?.let { exception.addSuppressed(it) }
        }
    }

    private data class OriginalExpenseState(
        val expense: Expense,
        val contribution: Contribution?,
        val withdrawals: List<CashWithdrawal>
    )
}
