package es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.handler

import es.pedrazamiguez.splittrip.core.common.constant.AppConstants
import es.pedrazamiguez.splittrip.domain.enums.SplitType
import es.pedrazamiguez.splittrip.domain.model.Subunit
import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.domain.service.split.SplitPreviewService
import es.pedrazamiguez.splittrip.features.expense.presentation.mapper.AddExpenseSplitUiMapper
import es.pedrazamiguez.splittrip.features.expense.presentation.model.SplitUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.action.AddExpenseUiAction
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.AddExpenseUiState
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * Handles subunit-aware expense split events (Level 1 entity splits + Level 2 intra-subunit).
 *
 * Operates on [AddExpenseUiState.entitySplits] when [AddExpenseUiState.isSubunitMode] is true.
 * Each entity row is a [SplitUiModel] where:
 * - Solo users: [SplitUiModel.isEntityRow] = true, [SplitUiModel.entityMembers] is empty.
 * - Subunits: [SplitUiModel.isEntityRow] = true, [SplitUiModel.entityMembers] holds member rows.
 *
 * Level 2 (intra-subunit) recalculation is delegated to [IntraSubunitSplitDelegate].
 */
// 13 public event methods (one per UI event) + 6 private helpers — function count
// is proportional to the two-level split event surface
@Suppress("TooManyFunctions")
class SubunitSplitEventHandler(
    private val splitPreviewService: SplitPreviewService,
    private val addExpenseSplitMapper: AddExpenseSplitUiMapper,
    private val intraSubunitSplitDelegate: IntraSubunitSplitDelegate,
    private val splitRowMappingDelegate: SplitRowMappingDelegate
) : AddExpenseEventHandler {

    private lateinit var _uiState: MutableStateFlow<AddExpenseUiState>
    private lateinit var _actions: MutableSharedFlow<AddExpenseUiAction>
    private lateinit var scope: CoroutineScope

    /** Cached subunits for the current group — used by [IntraSubunitSplitDelegate]. */
    private var groupSubunits: List<Subunit> = emptyList()

    override fun bind(
        stateFlow: MutableStateFlow<AddExpenseUiState>,
        actionsFlow: MutableSharedFlow<AddExpenseUiAction>,
        scope: CoroutineScope
    ) {
        _uiState = stateFlow
        _actions = actionsFlow
        this.scope = scope
    }

    // ── Initialization ──────────────────────────────────────────────────

    /**
     * Builds the initial entity-level split rows from group members and subunits.
     * Called from [ConfigEventHandler] after loading the group config.
     */
    fun initEntitySplits(
        memberIds: List<String>,
        subunits: List<Subunit>,
        memberProfiles: Map<String, User> = emptyMap()
    ) {
        groupSubunits = subunits
        if (subunits.isEmpty()) return

        val subunitMemberIds = subunits.flatMap { it.memberIds }.toSet()
        val soloMemberIds = memberIds.filter { it !in subunitMemberIds }

        val defaultSplitType = _uiState.value.availableSplitTypes
            .find { it.id == SplitType.EQUAL.name }

        val entityRows = mutableListOf<SplitUiModel>()

        // Solo members — entity rows without nested members
        for (userId in soloMemberIds) {
            entityRows.add(
                SplitUiModel(
                    userId = userId,
                    displayName = addExpenseSplitMapper.resolveDisplayName(userId, memberProfiles),
                    isEntityRow = true
                )
            )
        }

        // Subunit entity rows with nested member rows
        for (subunit in subunits) {
            val memberRows = subunit.memberIds.map { memberId ->
                SplitUiModel(
                    userId = memberId,
                    displayName = addExpenseSplitMapper.resolveDisplayName(memberId, memberProfiles),
                    subunitId = subunit.id
                )
            }.sortedWith(
                compareBy(String.CASE_INSENSITIVE_ORDER) { it.displayName }
            ).toImmutableList()

            entityRows.add(
                SplitUiModel(
                    userId = subunit.id,
                    displayName = subunit.name,
                    isEntityRow = true,
                    entityMembers = memberRows,
                    entitySplitType = defaultSplitType
                )
            )
        }

        // Sort entity rows: solo members first, then subunits, alphabetically within each group
        val sortedEntityRows = entityRows
            .sortedWith(
                compareBy<SplitUiModel> { it.entityMembers.isNotEmpty() }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { it.displayName }
            )
            .toImmutableList()

        _uiState.update {
            it.copy(
                hasSubunits = true,
                entitySplits = sortedEntityRows
            )
        }
    }

    /**
     * Resets subunit state when the loaded group has no subunits.
     */
    fun clearEntitySplits() {
        groupSubunits = emptyList()
        _uiState.update {
            it.copy(
                hasSubunits = false,
                isSubunitMode = false,
                entitySplits = persistentListOf()
            )
        }
    }

    // ── Mode Toggle ─────────────────────────────────────────────────────

    /**
     * Disables subunit mode (idempotent). Called when the user selects a USER- or GROUP-scoped
     * withdrawal pool so the split step reverts to flat mode, consistent with the pool scope.
     *
     * Entity exclusions are intentionally left untouched — if the user manually re-enables
     * subunit mode later they can adjust from a clean slate via [handleSubunitModeToggled].
     */
    fun disableSubunitMode() {
        if (!_uiState.value.isSubunitMode) return
        _uiState.update { it.copy(isSubunitMode = false, splitError = null) }
    }

    /**
     * Applies a smart default when a SUBUNIT-scoped cash pool is selected:
     * - Enables subunit mode so the Split step shows entity-level rows.
     * - Excludes all entity rows except the one whose [SplitUiModel.userId] matches [poolSubunitId].
     * - Clears all share locks so redistribution starts fresh.
     * - Recalculates entity splits to reflect the single-subunit active set.
     *
     * No-op when [poolSubunitId] is null or the group has no entity splits (subunit-less groups
     * where [SubunitSplitEventHandler.initEntitySplits] was never called).
     */
    fun applySubunitPoolDefault(poolSubunitId: String?) {
        if (poolSubunitId == null || _uiState.value.entitySplits.isEmpty()) return

        val updatedSplits = _uiState.value.entitySplits.map { entity ->
            entity.copy(
                isExcluded = entity.userId != poolSubunitId,
                isShareLocked = false
            )
        }.toImmutableList()

        _uiState.update {
            it.copy(
                isSubunitMode = true,
                entitySplits = updatedSplits,
                splitError = null
            )
        }
        recalculateEntitySplits()
    }

    fun handleSubunitModeToggled() {
        val newMode = !_uiState.value.isSubunitMode
        _uiState.update { it.copy(isSubunitMode = newMode, splitError = null) }
        if (newMode) {
            recalculateEntitySplits()
        }
    }

    // ── Accordion Toggle ────────────────────────────────────────────────

    fun handleAccordionToggled(entityId: String) {
        val updatedSplits = _uiState.value.entitySplits.map { entity ->
            if (entity.userId == entityId && entity.entityMembers.isNotEmpty()) {
                entity.copy(isExpanded = !entity.isExpanded)
            } else {
                entity
            }
        }.toImmutableList()
        _uiState.update { it.copy(entitySplits = updatedSplits) }
    }

    // ── Level 1: Entity-Level Events ────────────────────────────────────

    fun handleEntityExcludedToggled(entityId: String) {
        // Clear all entity locks on exclude toggle — redistribution resets
        // Also clear nested member locks to keep behavior consistent across levels
        val updatedSplits = _uiState.value.entitySplits.map { entity ->
            val clearedMembers = entity.entityMembers.map { it.copy(isShareLocked = false) }.toImmutableList()
            if (entity.userId == entityId) {
                entity.copy(
                    isExcluded = !entity.isExcluded,
                    isShareLocked = false,
                    entityMembers = clearedMembers
                )
            } else {
                entity.copy(isShareLocked = false, entityMembers = clearedMembers)
            }
        }.toImmutableList()
        _uiState.update { it.copy(entitySplits = updatedSplits, splitError = null) }
        recalculateEntitySplits()
    }

    fun handleEntityShareLockToggled(entityId: String) {
        val updatedSplits = _uiState.value.entitySplits.map { entity ->
            if (entity.userId == entityId) {
                entity.copy(isShareLocked = !entity.isShareLocked)
            } else {
                entity
            }
        }.toImmutableList()
        _uiState.update { it.copy(entitySplits = updatedSplits) }
    }

    // Entity-level exact amount edit + locked redistribution
    fun handleEntityAmountChanged(entityId: String, typedAmount: String) {
        val state = _uiState.value
        val currencyCode = state.selectedCurrency?.code ?: AppConstants.DEFAULT_CURRENCY_CODE
        val decimalDigits = state.selectedCurrency?.decimalDigits ?: 2
        val sourceAmountCents = parseSourceAmountToCents()

        if (sourceAmountCents <= 0) {
            val updatedSplits = state.entitySplits.map { entity ->
                if (entity.userId == entityId) {
                    entity.copy(amountInput = typedAmount, isShareLocked = true)
                } else {
                    entity
                }
            }.toImmutableList()
            _uiState.update { it.copy(entitySplits = updatedSplits, splitError = null) }
            return
        }

        val typedCents = splitPreviewService.parseAmountToCents(typedAmount, decimalDigits)

        val mappedSplits = splitRowMappingDelegate.applyExactAmountUpdate(
            splits = state.entitySplits,
            editedUserId = entityId,
            typedAmount = typedAmount,
            typedCents = typedCents,
            sourceAmountCents = sourceAmountCents,
            currencyCode = currencyCode,
            decimalDigits = decimalDigits
        )

        val updatedSplits = mappedSplits.map { entity ->
            delegateIntraRecalculation(entity, currencyCode)
        }.toImmutableList()

        _uiState.update { it.copy(entitySplits = updatedSplits, splitError = null) }
    }

    // Entity-level percentage edit + locked redistribution
    fun handleEntityPercentageChanged(entityId: String, typedPercentage: String) {
        val state = _uiState.value
        val currencyCode = state.selectedCurrency?.code ?: AppConstants.DEFAULT_CURRENCY_CODE
        val sourceAmountCents = parseSourceAmountToCents()

        val mappedSplits = splitRowMappingDelegate.applyPercentageUpdate(
            splits = state.entitySplits,
            editedUserId = entityId,
            typedPercentage = typedPercentage,
            sourceAmountCents = sourceAmountCents,
            currencyCode = currencyCode
        )

        val updatedSplits = mappedSplits.map { entity ->
            delegateIntraRecalculation(entity, currencyCode)
        }.toImmutableList()

        _uiState.update { it.copy(entitySplits = updatedSplits, splitError = null) }
    }

    // ── Level 2: Intra-Subunit Events ──────────────────────────────────

    fun handleIntraSubunitSplitTypeChanged(subunitId: String, splitTypeId: String) {
        val selectedType = _uiState.value.availableSplitTypes.find { it.id == splitTypeId } ?: return
        val currencyCode = _uiState.value.selectedCurrency?.code ?: AppConstants.DEFAULT_CURRENCY_CODE

        val updatedSplits = _uiState.value.entitySplits.map { entity ->
            if (entity.userId == subunitId) {
                val clearedMembers = entity.entityMembers.map { it.copy(isShareLocked = false) }.toImmutableList()
                val updated = entity.copy(entitySplitType = selectedType, entityMembers = clearedMembers)
                delegateIntraRecalculation(updated, currencyCode)
            } else {
                entity
            }
        }.toImmutableList()

        _uiState.update { it.copy(entitySplits = updatedSplits, splitError = null) }
    }

    // Intra-subunit exact amount edit + locked redistribution
    fun handleIntraSubunitAmountChanged(subunitId: String, userId: String, typedAmount: String) {
        val state = _uiState.value
        val currencyCode = state.selectedCurrency?.code ?: AppConstants.DEFAULT_CURRENCY_CODE
        val decimalDigits = state.selectedCurrency?.decimalDigits ?: 2

        val updatedSplits = state.entitySplits.map { entity ->
            if (entity.userId == subunitId) {
                val subunitTotalCents = entity.amountCents
                val typedCents = splitPreviewService.parseAmountToCents(typedAmount, decimalDigits)

                val updatedMembers = splitRowMappingDelegate.applyExactAmountUpdate(
                    splits = entity.entityMembers,
                    editedUserId = userId,
                    typedAmount = typedAmount,
                    typedCents = typedCents,
                    sourceAmountCents = subunitTotalCents,
                    currencyCode = currencyCode,
                    decimalDigits = decimalDigits
                )

                entity.copy(entityMembers = updatedMembers)
            } else {
                entity
            }
        }.toImmutableList()

        _uiState.update { it.copy(entitySplits = updatedSplits, splitError = null) }
    }

    // Intra-subunit percentage edit + locked redistribution
    fun handleIntraSubunitPercentageChanged(subunitId: String, userId: String, typedPercentage: String) {
        val state = _uiState.value
        val currencyCode = state.selectedCurrency?.code ?: AppConstants.DEFAULT_CURRENCY_CODE

        val updatedSplits = state.entitySplits.map { entity ->
            if (entity.userId == subunitId) {
                val subunitTotalCents = entity.amountCents

                val updatedMembers = splitRowMappingDelegate.applyPercentageUpdate(
                    splits = entity.entityMembers,
                    editedUserId = userId,
                    typedPercentage = typedPercentage,
                    sourceAmountCents = subunitTotalCents,
                    currencyCode = currencyCode
                )

                entity.copy(entityMembers = updatedMembers)
            } else {
                entity
            }
        }.toImmutableList()

        _uiState.update { it.copy(entitySplits = updatedSplits, splitError = null) }
    }

    fun handleIntraSubunitShareLockToggled(subunitId: String, userId: String) {
        val updatedSplits = _uiState.value.entitySplits.map { entity ->
            if (entity.userId == subunitId) {
                val updatedMembers = entity.entityMembers.map { member ->
                    if (member.userId == userId) {
                        member.copy(isShareLocked = !member.isShareLocked)
                    } else {
                        member
                    }
                }.toImmutableList()
                entity.copy(entityMembers = updatedMembers)
            } else {
                entity
            }
        }.toImmutableList()
        _uiState.update { it.copy(entitySplits = updatedSplits) }
    }

    // ── Recalculation (cross-handler) ───────────────────────────────────

    /**
     * Recalculates entity-level splits based on the current split type and source amount.
     * Called when source amount changes, split type changes, or subunit mode is toggled.
     */
    fun recalculateEntitySplits() {
        val state = _uiState.value
        if (!state.isSubunitMode) return
        if (state.entitySplits.isEmpty()) return

        val splitType = state.selectedSplitType?.let { SplitType.fromString(it.id) } ?: return
        val currencyCode = state.selectedCurrency?.code ?: AppConstants.DEFAULT_CURRENCY_CODE
        val sourceAmountCents = parseSourceAmountToCents()
        val decimalDigits = state.selectedCurrency?.decimalDigits ?: 2

        val activeEntityIds = state.entitySplits
            .filter { !it.isExcluded }
            .map { it.userId }

        val updatedSplits = intraSubunitSplitDelegate.distributeEntitySplits(
            entitySplits = state.entitySplits,
            splitType = splitType,
            sourceAmountCents = sourceAmountCents,
            activeEntityIds = activeEntityIds,
            currencyCode = currencyCode,
            groupSubunits = groupSubunits,
            decimalDigits = decimalDigits
        ) ?: return

        _uiState.update { it.copy(entitySplits = updatedSplits, splitError = null) }
    }

    // ── Private helpers ─────────────────────────────────────────────────

    /**
     * Delegates intra-subunit recalculation to [IntraSubunitSplitDelegate].
     * Passes cached [groupSubunits] and the current currency's decimal digits.
     */
    private fun delegateIntraRecalculation(entity: SplitUiModel, currencyCode: String): SplitUiModel {
        val decimalDigits = _uiState.value.selectedCurrency?.decimalDigits ?: 2
        return intraSubunitSplitDelegate.recalculate(entity, currencyCode, groupSubunits, decimalDigits)
    }

    private fun parseSourceAmountToCents(): Long {
        val state = _uiState.value
        val decimalPlaces = state.selectedCurrency?.decimalDigits ?: 2
        return intraSubunitSplitDelegate.parseSourceAmountToCents(state.sourceAmount, decimalPlaces)
    }
}
