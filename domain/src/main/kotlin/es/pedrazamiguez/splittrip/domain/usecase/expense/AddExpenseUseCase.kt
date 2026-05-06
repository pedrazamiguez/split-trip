package es.pedrazamiguez.splittrip.domain.usecase.expense

import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.domain.enums.PaymentMethod
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

class AddExpenseUseCase(
    private val expenseRepository: ExpenseRepository,
    private val cashWithdrawalRepository: CashWithdrawalRepository,
    private val expenseCalculatorService: ExpenseCalculatorService,
    private val exchangeRateCalculationService: ExchangeRateCalculationService,
    private val groupMembershipService: GroupMembershipService,
    private val contributionRepository: ContributionRepository,
    private val authenticationService: AuthenticationService,
    private val addOnCalculationService: AddOnCalculationService
) {

    suspend operator fun invoke(
        groupId: String?,
        expense: Expense,
        pairedContributionScope: PayerType = PayerType.USER,
        pairedSubunitId: String? = null,
        preferredWithdrawalScope: PayerType? = null,
        preferredWithdrawalOwnerId: String? = null
    ): Result<Unit> = runCatching {
        require(!groupId.isNullOrBlank()) { "Group ID cannot be null or blank" }
        require(expense.sourceAmount > 0) { "Expense amount must be greater than zero" }
        require(expense.title.isNotBlank()) { "Expense title cannot be empty" }

        groupMembershipService.requireMembership(groupId)

        // Ensure the expense has a stable ID before any processing, so the
        // paired contribution can link to it reliably.
        val expenseWithId = if (expense.id.isBlank()) {
            expense.copy(id = UUID.randomUUID().toString())
        } else {
            expense
        }

        val expenseToSave: Expense
        if (expenseWithId.paymentMethod == PaymentMethod.CASH) {
            val fifoResult = computeCashFifoResult(
                groupId,
                expenseWithId,
                preferredWithdrawalScope,
                preferredWithdrawalOwnerId
            )

            // Use the precondition-aware write path. On CashConflictException the
            // repository rolls back the local write and re-throws — no withdrawal
            // Room update is needed in that case.
            val transactionCommitted = expenseRepository.addCashExpense(
                groupId,
                fifoResult.expense,
                fifoResult.expectedRemainingAmounts
            )

            // Update withdrawal Room entries only when the Firestore transaction committed.
            // If the transaction did not run (offline fallback, transactionCommitted == false),
            // skipping this prevents deducting withdrawals in the cloud without a matching
            // expense document (which would break cloud-side atomicity).
            if (transactionCommitted) {
                cashWithdrawalRepository.updateRemainingAmounts(groupId, fifoResult.updatedWithdrawals)
            }

            expenseToSave = fifoResult.expense
        } else {
            expenseRepository.addExpense(groupId, expenseWithId)
            expenseToSave = expenseWithId
        }

        if (expenseToSave.payerType == PayerType.USER) {
            try {
                createPairedContribution(
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

    /**
     * Computes FIFO cash allocation for a cash-funded expense without performing any writes.
     *
     * Captures [CashFifoResult.expectedRemainingAmounts] (the withdrawal remaining amounts
     * *before* FIFO consumption) for use as optimistic-locking preconditions in the Firestore
     * transaction. Also builds [CashFifoResult.updatedWithdrawals] with the post-FIFO
     * remaining amounts for the subsequent Room update.
     *
     * When [preferredScope] is set (user chose a pool via the pool-selection UI), queries only
     * that exact scope via [CashWithdrawalRepository.getAvailableWithdrawalsByExactScope].
     * Otherwise, falls back to the scope-priority logic in
     * [CashWithdrawalRepository.getAvailableWithdrawals].
     *
     * @throws InsufficientCashException if the available cash cannot cover the expense.
     */
    private suspend fun computeCashFifoResult(
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

        // Guard before delegating to the calculator so we can throw a strongly-typed
        // exception that carries raw cent values for proper formatting in the UI layer.
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

        // Capture the *original* remaining amounts as optimistic-locking preconditions
        // (the values the client observed before FIFO deduction).
        val withdrawalById = availableWithdrawals.associateBy { it.id }
        val consumedWithdrawalIds = fifoResult.tranches.map { it.withdrawalId }.toSet()
        val expectedRemainingAmounts = availableWithdrawals
            .filter { it.id in consumedWithdrawalIds }
            .associate { it.id to it.remainingAmount }

        // Build updated withdrawal objects with post-FIFO remaining amounts —
        // no DB reads needed since we already have the in-memory list.
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

    /**
     * Creates a paired contribution that offsets the out-of-pocket expense in the
     * balance engine. The contribution amount equals the effective group amount
     * (base + add-ons) and its scope/subunit are driven by the caller.
     *
     * Sanitizes the scope/subunit pair before persisting:
     * - SUBUNIT scope requires a non-blank [subunitId] (fail-fast).
     * - GROUP/USER scope forces [subunitId] to null (defensive sanitization).
     *
     * Full subunit membership validation (subunit exists, user is member) is
     * handled at the UI layer via [ContributionValidationService]; adding it here
     * would require injecting SubunitRepository for an I/O read on every expense save.
     */
    private suspend fun createPairedContribution(
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

    /**
     * Validates and sanitizes the scope/subunit pair to prevent invalid
     * contributions from being silently persisted.
     */
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
        else -> null // GROUP/USER must not carry a subunitId
    }

    /**
     * Holds the result of the FIFO cash allocation computation.
     *
     * @param expense The expense enriched with [Expense.cashTranches], [Expense.groupAmount],
     *   and [Expense.exchangeRate].
     * @param updatedWithdrawals The withdrawal objects with post-FIFO [CashWithdrawal.remainingAmount]
     *   applied. Used for the Room update after a successful commit.
     * @param expectedRemainingAmounts Map of withdrawal ID → `remainingAmount` observed *before*
     *   FIFO consumption. Used as optimistic-locking preconditions in the Firestore transaction.
     */
    private data class CashFifoResult(
        val expense: Expense,
        val updatedWithdrawals: List<CashWithdrawal>,
        val expectedRemainingAmounts: Map<String, Long>
    )
}
