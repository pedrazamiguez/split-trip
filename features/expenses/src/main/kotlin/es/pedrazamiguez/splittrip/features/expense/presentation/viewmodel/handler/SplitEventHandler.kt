package es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.handler

import es.pedrazamiguez.splittrip.core.common.constant.AppConstants
import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.FormattingHelper
import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.domain.enums.SplitType
import es.pedrazamiguez.splittrip.domain.service.split.ExpenseSplitCalculatorFactory
import es.pedrazamiguez.splittrip.domain.service.split.SplitPreviewService
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.action.AddExpenseUiAction
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.AddExpenseUiState
import java.math.BigDecimal
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import timber.log.Timber

/**
 * Handles expense split calculation events:
 * [SplitTypeChanged], [SplitAmountChanged], [SplitPercentageChanged], [SplitExcludedToggled].
 *
 * Also exposes [recalculateSplits] for cross-handler calls (e.g., when source amount changes).
 *
 * All BigDecimal math is delegated to [SplitPreviewService] (domain layer).
 */
class SplitEventHandler(
    private val splitCalculatorFactory: ExpenseSplitCalculatorFactory,
    private val splitPreviewService: SplitPreviewService,
    private val formattingHelper: FormattingHelper,
    private val splitRowMappingDelegate: SplitRowMappingDelegate
) : AddExpenseEventHandler {

    private lateinit var _uiState: MutableStateFlow<AddExpenseUiState>
    private lateinit var _actions: MutableSharedFlow<AddExpenseUiAction>
    private lateinit var scope: CoroutineScope

    override fun bind(
        stateFlow: MutableStateFlow<AddExpenseUiState>,
        actionsFlow: MutableSharedFlow<AddExpenseUiAction>,
        scope: CoroutineScope
    ) {
        _uiState = stateFlow
        _actions = actionsFlow
        this.scope = scope
    }

    fun handleSplitTypeChanged(splitTypeId: String) {
        val selectedSplitType = _uiState.value.availableSplitTypes
            .find { it.id == splitTypeId } ?: return
        // Clear all locks when switching split type (user is starting over)
        val clearedSplits = _uiState.value.splits.map { it.copy(isShareLocked = false) }.toImmutableList()
        _uiState.update {
            it.copy(
                selectedSplitType = selectedSplitType,
                splits = clearedSplits,
                splitError = null
            )
        }
        recalculateSplits()
        recomputePersonalCashWarning()
    }

    fun handleSplitExcludedToggled(userId: String) {
        val updatedSplits = _uiState.value.splits.map { split ->
            if (split.userId == userId) {
                // Clear lock when excluding/including a member
                split.copy(isExcluded = !split.isExcluded, isShareLocked = false)
            } else {
                split
            }
        }.toImmutableList()
        // Clear all locks on exclude toggle — redistribution resets
        val clearedSplits = updatedSplits.map { it.copy(isShareLocked = false) }.toImmutableList()
        _uiState.update { it.copy(splits = clearedSplits, splitError = null) }
        recalculateSplits()
        recomputePersonalCashWarning()
    }

    fun handleShareLockToggled(userId: String) {
        val updatedSplits = _uiState.value.splits.map { split ->
            if (split.userId == userId) {
                split.copy(isShareLocked = !split.isShareLocked)
            } else {
                split
            }
        }.toImmutableList()
        _uiState.update { it.copy(splits = updatedSplits) }
    }

    /**
     * Recalculates the per-user splits based on the current split type,
     * source amount, and active participants.
     *
     * - EQUAL: auto-calculates shares with currency display (e.g., "€16.67").
     * - EXACT: auto-distributes remainder evenly among users who haven't been edited.
     * - PERCENT: auto-distributes remaining percentage evenly among unedited users.
     */
    fun recalculateSplits() {
        val state = _uiState.value
        val splitType = state.selectedSplitType?.let { SplitType.fromString(it.id) } ?: return
        val currencyCode = state.selectedCurrency?.code ?: AppConstants.DEFAULT_CURRENCY_CODE

        val activeParticipantIds = state.splits
            .filter { !it.isExcluded }
            .map { it.userId }
        if (activeParticipantIds.isEmpty()) return

        val sourceAmountCents = parseSourceAmountToCents()

        when (splitType) {
            SplitType.EQUAL -> recalculateEqualSplits(
                sourceAmountCents,
                activeParticipantIds,
                currencyCode
            )

            SplitType.EXACT -> recalculateExactSplits(
                sourceAmountCents,
                activeParticipantIds,
                currencyCode
            )

            SplitType.PERCENT -> recalculatePercentSplits(
                sourceAmountCents,
                activeParticipantIds,
                currencyCode
            )
        }
    }

    /**
     * Handles EXACT mode: user typed an amount for one member.
     * Auto-distributes the remaining amount evenly among the other active members.
     */
    fun handleExactAmountChanged(editedUserId: String, typedAmount: String) {
        val state = _uiState.value
        val currencyCode = state.selectedCurrency?.code ?: AppConstants.DEFAULT_CURRENCY_CODE
        val decimalDigits = state.selectedCurrency?.decimalDigits ?: 2
        val sourceAmountCents = parseSourceAmountToCents()
        if (sourceAmountCents <= 0) {
            // Just store the typed value and auto-lock, nothing to distribute
            val updatedSplits = state.splits.map { split ->
                if (split.userId == editedUserId) {
                    split.copy(amountInput = typedAmount, isShareLocked = true)
                } else {
                    split
                }
            }.toImmutableList()
            _uiState.update { it.copy(splits = updatedSplits, splitError = null) }
            return
        }

        val typedCents = splitPreviewService.parseAmountToCents(typedAmount, decimalDigits)

        val updatedSplits = splitRowMappingDelegate.applyExactAmountUpdate(
            splits = state.splits,
            editedUserId = editedUserId,
            typedAmount = typedAmount,
            typedCents = typedCents,
            sourceAmountCents = sourceAmountCents,
            currencyCode = currencyCode,
            decimalDigits = decimalDigits
        )

        _uiState.update { it.copy(splits = updatedSplits, splitError = null) }
    }

    /**
     * Handles PERCENT mode: user typed a percentage for one member.
     * Auto-distributes the remaining percentage evenly among the other active members.
     */
    fun handlePercentageChanged(editedUserId: String, typedPercentage: String) {
        val state = _uiState.value
        val currencyCode = state.selectedCurrency?.code ?: AppConstants.DEFAULT_CURRENCY_CODE
        val sourceAmountCents = parseSourceAmountToCents()

        val updatedSplits = splitRowMappingDelegate.applyPercentageUpdate(
            splits = state.splits,
            editedUserId = editedUserId,
            typedPercentage = typedPercentage,
            sourceAmountCents = sourceAmountCents,
            currencyCode = currencyCode
        )

        _uiState.update { it.copy(splits = updatedSplits, splitError = null) }
    }

    // ── Pool-aware smart defaults ──────────────────────────────────────

    /**
     * Applies a smart split default based on the selected withdrawal pool scope.
     *
     * - USER pool  → exclude everyone except the pool owner (identified by [poolOwnerId],
     *   falling back to [currentUserId] when ownerId is null).
     * - SUBUNIT pool → exclude all members whose [SplitUiModel.subunitId] ≠ [poolOwnerId].
     * - GROUP pool or null → no-op on splits; only resets [AddExpenseUiState.personalCashSplitWarning].
     *
     * Also resets [AddExpenseUiState.personalCashSplitWarning] to null immediately after
     * pre-fill, because the freshly applied exclusions are always pool-scope–compatible.
     */
    fun applyPersonalPoolSplitDefault(
        poolScope: PayerType,
        poolOwnerId: String?,
        currentUserId: String?
    ) {
        when (poolScope) {
            PayerType.USER -> {
                val ownerId = poolOwnerId ?: currentUserId
                val updatedSplits = _uiState.value.splits.map { split ->
                    if (split.isEntityRow) {
                        split
                    } else {
                        split.copy(isExcluded = split.userId != ownerId, isShareLocked = false)
                    }
                }.toImmutableList()
                _uiState.update { it.copy(splits = updatedSplits, personalCashSplitWarning = null) }
                recalculateSplits()
            }

            PayerType.SUBUNIT -> {
                val updatedSplits = _uiState.value.splits.map { split ->
                    if (split.isEntityRow) {
                        split
                    } else {
                        split.copy(isExcluded = split.subunitId != poolOwnerId, isShareLocked = false)
                    }
                }.toImmutableList()
                _uiState.update { it.copy(splits = updatedSplits, personalCashSplitWarning = null) }
                recalculateSplits()
            }

            PayerType.GROUP -> {
                // GROUP pool funds a shared expense — every member should participate.
                // Clearing exclusions here handles the case where the user first selected a
                // USER/SUBUNIT pool (which excluded non-pool members) and then switched back
                // to GROUP cash; without the reset those exclusions would silently persist.
                val updatedSplits = _uiState.value.splits.map { split ->
                    if (split.isEntityRow) {
                        split
                    } else {
                        split.copy(isExcluded = false, isShareLocked = false)
                    }
                }.toImmutableList()
                _uiState.update { it.copy(splits = updatedSplits, personalCashSplitWarning = null) }
                recalculateSplits()
            }
        }
    }

    /**
     * Recomputes [AddExpenseUiState.personalCashSplitWarning].
     *
     * Non-null when a USER- or SUBUNIT-scoped pool is selected AND at least one included member
     * (flat mode) or entity (subunit mode) falls outside the pool's natural scope —
     * i.e., the user is spending personal/subunit cash for someone else.
     *
     * - **USER pool / flat mode:** any included `split` whose `userId` ≠ pool owner.
     * - **SUBUNIT pool / flat mode:** any included `split` whose `subunitId` ≠ pool subunit.
     * - **SUBUNIT pool / subunit mode:** any included *entity* whose `userId` ≠ pool subunit ID.
     * - **GROUP pool or no pool:** never warn.
     *
     * `internal` so [AddExpenseViewModel] can call it after entity-level exclusion toggles
     * (which are handled by [SubunitSplitEventHandler], not this handler).
     */
    internal fun recomputePersonalCashWarning() {
        val state = _uiState.value
        val pool = state.selectedWithdrawalPool
        val warning: UiText? = when (pool?.scope) {
            PayerType.USER -> {
                val hasOutsideMembers = state.splits.any { split ->
                    !split.isExcluded && !split.isEntityRow && split.userId != pool.ownerId
                }
                if (hasOutsideMembers) {
                    UiText.StringResource(R.string.add_expense_personal_cash_split_warning)
                } else {
                    null
                }
            }

            PayerType.SUBUNIT -> {
                val hasOutsideEntities = if (state.isSubunitMode) {
                    // In subunit mode the user sees entity rows. Warn if any entity outside the
                    // pool's subunit is included (regardless of whether it's a subunit or solo user).
                    state.entitySplits.any { entity ->
                        !entity.isExcluded && entity.userId != pool.ownerId
                    }
                } else {
                    // Flat mode: check per-member subunit membership.
                    state.splits.any { split ->
                        !split.isExcluded && !split.isEntityRow && split.subunitId != pool.ownerId
                    }
                }
                if (hasOutsideEntities) {
                    UiText.StringResource(
                        R.string.add_expense_subunit_cash_split_warning,
                        pool.displayLabel
                    )
                } else {
                    null
                }
            }

            // GROUP pool or no pool — never warn.
            else -> null
        }
        _uiState.update { it.copy(personalCashSplitWarning = warning) }
    }

    // ── Private helpers ──────────────────────────────────────────────────

    /**
     * EQUAL: Splits the total evenly, showing amounts with currency symbol (read-only).
     */
    private fun recalculateEqualSplits(
        sourceAmountCents: Long,
        activeParticipantIds: List<String>,
        currencyCode: String
    ) {
        if (sourceAmountCents <= 0) return

        try {
            val calculator = splitCalculatorFactory.create(SplitType.EQUAL)
            val sharesByUserId = calculator.calculateShares(sourceAmountCents, activeParticipantIds)
                .associateBy { it.userId }

            val state = _uiState.value
            val updatedSplits = state.splits.map { uiModel ->
                val share = sharesByUserId[uiModel.userId]
                if (share != null && !uiModel.isExcluded) {
                    uiModel.copy(
                        amountCents = share.amountCents,
                        formattedAmount = formattingHelper.formatCentsWithCurrency(
                            share.amountCents,
                            currencyCode
                        )
                    )
                } else if (uiModel.isExcluded) {
                    uiModel.copy(amountCents = 0L, formattedAmount = "")
                } else {
                    uiModel
                }
            }.toImmutableList()

            _uiState.update { it.copy(splits = updatedSplits, splitError = null) }
        } catch (e: Exception) {
            Timber.w(e, "Failed to calculate equal splits")
        }
    }

    /**
     * EXACT: Auto-distributes the remaining amount evenly among the
     * other active members. The user who was just edited keeps their typed value.
     * Called from [recalculateSplits] when switching types (resets all to even).
     */
    private fun recalculateExactSplits(
        sourceAmountCents: Long,
        activeParticipantIds: List<String>,
        currencyCode: String
    ) {
        if (sourceAmountCents <= 0 || activeParticipantIds.isEmpty()) return

        // When switching to EXACT mode, start with an even split
        try {
            val calculator = splitCalculatorFactory.create(SplitType.EQUAL)
            val sharesByUserId = calculator.calculateShares(sourceAmountCents, activeParticipantIds)
                .associateBy { it.userId }

            val decimalDigits = _uiState.value.selectedCurrency?.decimalDigits ?: 2
            val state = _uiState.value
            val updatedSplits = state.splits.map { uiModel ->
                val share = sharesByUserId[uiModel.userId]
                if (share != null && !uiModel.isExcluded) {
                    uiModel.copy(
                        amountCents = share.amountCents,
                        amountInput = formattingHelper.formatCentsValue(share.amountCents, decimalDigits),
                        formattedAmount = formattingHelper.formatCentsWithCurrency(
                            share.amountCents,
                            currencyCode
                        )
                    )
                } else if (uiModel.isExcluded) {
                    uiModel.copy(amountCents = 0L, amountInput = "", formattedAmount = "")
                } else {
                    uiModel
                }
            }.toImmutableList()

            _uiState.update { it.copy(splits = updatedSplits, splitError = null) }
        } catch (e: Exception) {
            Timber.w(e, "Failed to calculate initial exact splits")
        }
    }

    /**
     * PERCENT: Auto-distributes 100% evenly among active members.
     * Called from [recalculateSplits] when switching types (resets all to even).
     */
    private fun recalculatePercentSplits(
        sourceAmountCents: Long,
        activeParticipantIds: List<String>,
        currencyCode: String
    ) {
        if (activeParticipantIds.isEmpty()) return

        val sharesByUserId = splitPreviewService.distributePercentagesEvenly(
            sourceAmountCents,
            activeParticipantIds
        ).associateBy { it.userId }

        val state = _uiState.value
        val updatedSplits = state.splits.map { uiModel ->
            val share = sharesByUserId[uiModel.userId]
            if (!uiModel.isExcluded && share != null) {
                val pct = share.percentage ?: BigDecimal.ZERO
                uiModel.copy(
                    percentageInput = formattingHelper.formatPercentageForDisplay(pct),
                    amountCents = share.amountCents,
                    formattedAmount = if (sourceAmountCents > 0) {
                        formattingHelper.formatCentsWithCurrency(share.amountCents, currencyCode)
                    } else {
                        ""
                    }
                )
            } else if (uiModel.isExcluded) {
                uiModel.copy(percentageInput = "", amountCents = 0L, formattedAmount = "")
            } else {
                uiModel
            }
        }.toImmutableList()

        _uiState.update { it.copy(splits = updatedSplits, splitError = null) }
    }

    /**
     * Parses the current source amount from UiState to cents.
     */
    private fun parseSourceAmountToCents(): Long {
        val state = _uiState.value
        val decimalPlaces = state.selectedCurrency?.decimalDigits ?: 2
        return splitPreviewService.parseAmountToCents(state.sourceAmount.trim(), decimalPlaces)
    }
}
