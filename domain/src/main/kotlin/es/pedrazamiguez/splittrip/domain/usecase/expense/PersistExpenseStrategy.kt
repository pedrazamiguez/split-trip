package es.pedrazamiguez.splittrip.domain.usecase.expense

import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.domain.exception.InsufficientCashException
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
import java.util.UUID

interface PersistExpenseStrategy {
    suspend fun persist(
        groupId: String,
        expense: Expense,
        pairedContributionScope: PayerType,
        pairedSubunitId: String?,
        preferredWithdrawalScope: PayerType?,
        preferredWithdrawalOwnerId: String?
    ): Result<Unit>
}

abstract class BasePersistExpenseStrategy(
    protected val expenseRepository: ExpenseRepository,
    protected val cashWithdrawalRepository: CashWithdrawalRepository,
    protected val expenseCalculatorService: ExpenseCalculatorService,
    protected val exchangeRateCalculationService: ExchangeRateCalculationService,
    protected val groupMembershipService: GroupMembershipService,
    protected val contributionRepository: ContributionRepository,
    protected val authenticationService: AuthenticationService,
    protected val addOnCalculationService: AddOnCalculationService
) : PersistExpenseStrategy {

    protected suspend fun computeCashFifoResult(
        groupId: String,
        expense: Expense,
        preferredScope: PayerType? = null,
        preferredScopeOwnerId: String? = null
    ): CashFifoResult {
        val availableWithdrawals = if (preferredScope != null) {
            cashWithdrawalRepository.getAvailableWithdrawalsByExactScope(
                groupId = groupId,
                currency = expense.sourceCurrency,
                scope = preferredScope,
                scopeOwnerId = preferredScopeOwnerId
            )
        } else {
            cashWithdrawalRepository.getAvailableWithdrawals(
                groupId,
                expense.sourceCurrency,
                expense.payerType,
                expense.payerId
            )
        }

        if (expenseCalculatorService.hasInsufficientCash(expense.sourceAmount, availableWithdrawals)) {
            val availableCents = availableWithdrawals.sumOf { it.remainingAmount }
            throw InsufficientCashException(
                requiredCents = expense.sourceAmount,
                availableCents = availableCents
            )
        }

        val fifoResult = expenseCalculatorService.calculateFifoCashAmount(
            amountToCover = expense.sourceAmount,
            availableWithdrawals = availableWithdrawals
        )

        val withdrawalById = availableWithdrawals.associateBy { it.id }
        val consumedWithdrawalIds = fifoResult.tranches.map { it.withdrawalId }.toSet()
        val expectedRemainingAmounts = availableWithdrawals
            .filter { it.id in consumedWithdrawalIds }
            .associate { it.id to it.remainingAmount }

        val updatedWithdrawals = fifoResult.tranches.map { tranche ->
            val withdrawal = withdrawalById.getValue(tranche.withdrawalId)
            withdrawal.copy(remainingAmount = withdrawal.remainingAmount - tranche.amountConsumed)
        }

        val updatedExpense = expense.copy(
            cashTranches = fifoResult.tranches,
            groupAmount = fifoResult.groupAmountCents,
            exchangeRate = exchangeRateCalculationService.calculateBlendedRate(
                sourceAmountCents = expense.sourceAmount,
                groupAmountCents = fifoResult.groupAmountCents
            )
        )

        return CashFifoResult(
            expense = updatedExpense,
            updatedWithdrawals = updatedWithdrawals,
            expectedRemainingAmounts = expectedRemainingAmounts
        )
    }

    protected suspend fun createPairedContribution(
        groupId: String,
        expense: Expense,
        contributionScope: PayerType,
        subunitId: String?
    ) {
        val sanitizedSubunitId = sanitizeSubunitId(contributionScope, subunitId)

        val effectiveAmount = addOnCalculationService.calculateEffectiveGroupAmount(
            expense.groupAmount,
            expense.addOns
        )
        val createdBy = authenticationService.requireUserId()
        val userId = expense.payerId ?: createdBy
        val pairedContribution = Contribution(
            id = UUID.randomUUID().toString(),
            groupId = groupId,
            userId = userId,
            createdBy = createdBy,
            contributionScope = contributionScope,
            subunitId = sanitizedSubunitId,
            amount = effectiveAmount,
            currency = expense.groupCurrency,
            linkedExpenseId = expense.id
        )
        contributionRepository.addContribution(groupId, pairedContribution)
    }

    private fun sanitizeSubunitId(
        contributionScope: PayerType,
        subunitId: String?
    ): String? = when (contributionScope) {
        PayerType.SUBUNIT -> {
            require(!subunitId.isNullOrBlank()) {
                "SUBUNIT scope requires a non-blank subunitId"
            }
            subunitId
        }
        else -> null
    }

    protected data class CashFifoResult(
        val expense: Expense,
        val updatedWithdrawals: List<CashWithdrawal>,
        val expectedRemainingAmounts: Map<String, Long>
    )
}
