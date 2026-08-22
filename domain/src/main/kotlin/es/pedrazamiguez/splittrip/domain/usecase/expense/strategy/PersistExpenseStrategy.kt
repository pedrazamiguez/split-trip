package es.pedrazamiguez.splittrip.domain.usecase.expense.strategy

import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.domain.enums.PaymentMethod
import es.pedrazamiguez.splittrip.domain.enums.PaymentStatus
import es.pedrazamiguez.splittrip.domain.exception.InsufficientCashException
import es.pedrazamiguez.splittrip.domain.model.CashWithdrawal
import es.pedrazamiguez.splittrip.domain.model.Contribution
import es.pedrazamiguez.splittrip.domain.model.Expense
import es.pedrazamiguez.splittrip.domain.model.SubExpense
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

    protected fun hasActiveCashPayment(expense: Expense): Boolean = if (expense.isComposite) {
        expense.subExpenses.any {
            it.paymentMethod == PaymentMethod.CASH && it.paymentStatus != PaymentStatus.CANCELLED
        }
    } else {
        expense.paymentMethod == PaymentMethod.CASH && expense.paymentStatus != PaymentStatus.CANCELLED
    }

    protected fun hasActiveUserPayer(expense: Expense): Boolean = if (expense.isComposite) {
        expense.subExpenses.any {
            it.payerType == PayerType.USER && it.paymentStatus != PaymentStatus.CANCELLED
        }
    } else {
        expense.payerType == PayerType.USER && expense.paymentStatus != PaymentStatus.CANCELLED
    }

    protected suspend fun computeFifoResult(
        groupId: String,
        expense: Expense,
        preferredWithdrawalScope: PayerType?,
        preferredWithdrawalOwnerId: String?
    ): CashFifoResult = if (expense.isComposite) {
        computeCompositeCashFifoResult(
            groupId,
            expense,
            preferredWithdrawalScope,
            preferredWithdrawalOwnerId
        )
    } else {
        computeCashFifoResult(
            groupId,
            expense,
            preferredWithdrawalScope,
            preferredWithdrawalOwnerId
        )
    }

    protected suspend fun computeCompositeCashFifoResult(
        groupId: String,
        expense: Expense,
        preferredScope: PayerType? = null,
        preferredScopeOwnerId: String? = null
    ): CashFifoResult {
        val allUpdatedWithdrawalsMap = mutableMapOf<String, CashWithdrawal>()
        val allExpectedRemainingAmounts = mutableMapOf<String, Long>()
        val updatedSubExpenses = mutableListOf<SubExpense>()

        for (subExpense in expense.subExpenses) {
            if (subExpense.paymentMethod != PaymentMethod.CASH || subExpense.paymentStatus == PaymentStatus.CANCELLED) {
                updatedSubExpenses.add(subExpense)
                continue
            }
            val updatedSub = processSubExpenseCashFifo(
                groupId = groupId,
                subExpense = subExpense,
                preferredScope = preferredScope,
                preferredScopeOwnerId = preferredScopeOwnerId,
                allUpdatedWithdrawalsMap = allUpdatedWithdrawalsMap,
                allExpectedRemainingAmounts = allExpectedRemainingAmounts
            )
            updatedSubExpenses.add(updatedSub)
        }

        val updatedExpense = expense.copy(
            subExpenses = updatedSubExpenses,
            groupAmount = updatedSubExpenses.sumOf { it.groupAmountCents }
        )

        return CashFifoResult(
            expense = updatedExpense,
            updatedWithdrawals = allUpdatedWithdrawalsMap.values.toList(),
            expectedRemainingAmounts = allExpectedRemainingAmounts
        )
    }

    private suspend fun processSubExpenseCashFifo(
        groupId: String,
        subExpense: SubExpense,
        preferredScope: PayerType?,
        preferredScopeOwnerId: String?,
        allUpdatedWithdrawalsMap: MutableMap<String, CashWithdrawal>,
        allExpectedRemainingAmounts: MutableMap<String, Long>
    ): SubExpense {
        val availableWithdrawals = if (preferredScope != null) {
            cashWithdrawalRepository.getAvailableWithdrawalsByExactScope(
                groupId = groupId,
                currency = subExpense.currency,
                scope = preferredScope,
                scopeOwnerId = preferredScopeOwnerId
            )
        } else {
            cashWithdrawalRepository.getAvailableWithdrawals(
                groupId,
                subExpense.currency,
                subExpense.payerType,
                subExpense.payerId
            )
        }.map { w ->
            allUpdatedWithdrawalsMap[w.id] ?: w
        }.filter { it.remainingAmount > 0 }

        if (expenseCalculatorService.hasInsufficientCash(subExpense.amountCents, availableWithdrawals)) {
            val availableCents = availableWithdrawals.sumOf { it.remainingAmount }
            throw InsufficientCashException(
                requiredCents = subExpense.amountCents,
                availableCents = availableCents
            )
        }

        val fifoResult = expenseCalculatorService.calculateFifoCashAmount(
            amountToCover = subExpense.amountCents,
            availableWithdrawals = availableWithdrawals
        )

        val withdrawalById = availableWithdrawals.associateBy { it.id }
        val consumedWithdrawalIds = fifoResult.tranches.map { it.withdrawalId }.toSet()
        availableWithdrawals
            .filter { it.id in consumedWithdrawalIds && it.id !in allExpectedRemainingAmounts }
            .forEach { allExpectedRemainingAmounts[it.id] = it.remainingAmount }

        fifoResult.tranches.forEach { tranche ->
            val withdrawal = allUpdatedWithdrawalsMap[tranche.withdrawalId]
                ?: withdrawalById.getValue(tranche.withdrawalId)
            allUpdatedWithdrawalsMap[tranche.withdrawalId] = withdrawal.copy(
                remainingAmount = withdrawal.remainingAmount - tranche.amountConsumed
            )
        }

        return subExpense.copy(
            cashTranches = fifoResult.tranches,
            groupAmountCents = fifoResult.groupAmountCents,
            exchangeRate = exchangeRateCalculationService.calculateBlendedRate(
                sourceAmountCents = subExpense.amountCents,
                groupAmountCents = fifoResult.groupAmountCents
            )
        )
    }

    protected suspend fun createPairedContributions(
        groupId: String,
        expense: Expense,
        contributionScope: PayerType,
        subunitId: String?
    ) {
        if (expense.subExpenses.isEmpty()) {
            if (expense.payerType == PayerType.USER && expense.paymentStatus != PaymentStatus.CANCELLED) {
                createPairedContribution(groupId, expense, contributionScope, subunitId)
            }
        } else {
            val sanitizedSubunitId = sanitizeSubunitId(contributionScope, subunitId)
            val createdBy = authenticationService.requireUserId()
            for (subExpense in expense.subExpenses) {
                if (subExpense.payerType == PayerType.USER && subExpense.paymentStatus != PaymentStatus.CANCELLED) {
                    val effectiveAmount = addOnCalculationService.calculateEffectiveGroupAmount(
                        subExpense.groupAmountCents,
                        subExpense.addOns
                    )
                    val userId = subExpense.payerId ?: createdBy
                    val pairedContribution = Contribution(
                        id = UUID.randomUUID().toString(),
                        groupId = groupId,
                        userId = userId,
                        createdBy = createdBy,
                        contributionScope = contributionScope,
                        subunitId = sanitizedSubunitId,
                        amount = effectiveAmount,
                        currency = expense.groupCurrency,
                        linkedExpenseId = expense.id,
                        createdAt = subExpense.operationDate ?: expense.createdAt
                    )
                    contributionRepository.addContribution(groupId, pairedContribution)
                }
            }
        }
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
            linkedExpenseId = expense.id,
            createdAt = expense.createdAt
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
