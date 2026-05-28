package es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.handler

import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.model.CurrencyUiModel
import es.pedrazamiguez.splittrip.core.designsystem.presentation.model.SubunitOptionUiModel
import es.pedrazamiguez.splittrip.domain.enums.ExpenseCategory
import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.domain.enums.PaymentMethod
import es.pedrazamiguez.splittrip.domain.enums.PaymentStatus
import es.pedrazamiguez.splittrip.domain.enums.SplitType
import es.pedrazamiguez.splittrip.domain.model.ExtractionCapability
import es.pedrazamiguez.splittrip.domain.model.GroupExpenseConfig
import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.service.ReceiptExtractionService
import es.pedrazamiguez.splittrip.domain.usecase.expense.GetGroupExpenseConfigUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.GetGroupLastUsedCategoryUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.GetGroupLastUsedCurrencyUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.GetGroupLastUsedPaymentMethodUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.GetMemberProfilesUseCase
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.mapper.AddExpenseOptionsUiMapper
import es.pedrazamiguez.splittrip.features.expense.presentation.mapper.AddExpenseSplitUiMapper
import es.pedrazamiguez.splittrip.features.expense.presentation.mapper.AddExpenseUiMapper
import es.pedrazamiguez.splittrip.features.expense.presentation.model.CategoryUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.model.FundingSourceUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.model.PaymentMethodUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.model.PaymentStatusUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.model.SplitTypeUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.action.AddExpenseUiAction
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.AddExpenseStep
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.AddExpenseUiState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Handles group configuration loading events:
 * [LoadGroupConfig], [RetryLoadConfig].
 *
 * Post-config actions (exchange rate fetching, entity split initialization)
 * are emitted via [postConfigCallback] and routed by the ViewModel to the
 * appropriate handler — avoiding handler-to-handler constructor coupling.
 */
@Suppress("LongParameterList")
class ConfigEventHandler(
    private val getGroupExpenseConfigUseCase: GetGroupExpenseConfigUseCase,
    private val getGroupLastUsedCurrencyUseCase: GetGroupLastUsedCurrencyUseCase,
    private val getGroupLastUsedPaymentMethodUseCase: GetGroupLastUsedPaymentMethodUseCase,
    private val getGroupLastUsedCategoryUseCase: GetGroupLastUsedCategoryUseCase,
    private val getMemberProfilesUseCase: GetMemberProfilesUseCase,
    private val authenticationService: AuthenticationService,
    private val addExpenseOptionsMapper: AddExpenseOptionsUiMapper,
    private val addExpenseSplitMapper: AddExpenseSplitUiMapper,
    private val addExpenseUiMapper: AddExpenseUiMapper,
    private val receiptExtractionService: ReceiptExtractionService
) : AddExpenseEventHandler {

    private lateinit var _uiState: MutableStateFlow<AddExpenseUiState>
    private lateinit var _actions: MutableSharedFlow<AddExpenseUiAction>
    private lateinit var scope: CoroutineScope

    /**
     * Callback for post-config actions that require cross-handler communication.
     * Set by the ViewModel during initialization via [setPostConfigCallback].
     */
    private var postConfigCallback: ((PostConfigAction) -> Unit)? = null

    override fun bind(
        stateFlow: MutableStateFlow<AddExpenseUiState>,
        actionsFlow: MutableSharedFlow<AddExpenseUiAction>,
        scope: CoroutineScope
    ) {
        _uiState = stateFlow
        _actions = actionsFlow
        this.scope = scope
    }

    /**
     * Registers the callback the ViewModel uses to route post-config actions
     * to the appropriate sibling handlers.
     */
    fun setPostConfigCallback(callback: (PostConfigAction) -> Unit) {
        postConfigCallback = callback
    }

    suspend fun suspendLoadGroupConfig(groupId: String, forceRefresh: Boolean = false) {
        val currentState = _uiState.value
        val isGroupChanged = currentState.loadedGroupId != groupId

        // Optimization: Don't reload if we already have data for the SAME group (e.g., on screen rotation)
        // unless forceRefresh is explicitly requested (e.g., retry after error)
        // Always reload if the groupId has changed
        if (!forceRefresh && !isGroupChanged && currentState.isConfigLoaded) return

        // Reset form state when loading a different group's config, but preserve the
        // edit-mode bootstrap fields set by the ViewModel constructor from the strategy.
        // Wiping these would cause the screen to revert to "add expense" titles/labels,
        // re-enable the AI receipt-scan step, and disable forward step jumps.
        if (isGroupChanged) {
            _uiState.update { current ->
                AddExpenseUiState(
                    isLoading = true,
                    configLoadFailed = false,
                    isEditMode = current.isEditMode,
                    screenTitleRes = current.screenTitleRes,
                    submitLabelRes = current.submitLabelRes
                )
            }
        } else {
            _uiState.update { it.copy(isLoading = true, configLoadFailed = false) }
        }

        val config = getGroupExpenseConfigUseCase(groupId, forceRefresh).getOrElse { e ->
            Timber.e(e, "Failed to load group configuration for groupId: $groupId")
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isConfigLoaded = false,
                    configLoadFailed = true,
                    error = UiText.StringResource(R.string.expense_error_load_group_config)
                )
            }
            return
        }
        applyConfig(groupId, config)
    }

    fun loadGroupConfig(groupId: String?, forceRefresh: Boolean = false) {
        if (groupId == null) return
        scope.launch {
            suspendLoadGroupConfig(groupId, forceRefresh)
        }
    }

    /**
     * Maps the loaded [GroupExpenseConfig] into UI state, resolves user preferences
     * (last-used currency, payment method, category), and emits post-config actions.
     */
    // Single sequential state-assembly function: each `copy(...)` field is a distinct
    // mapping from the loaded config; splitting would obscure the data-flow with no benefit.
    @Suppress("LongMethod")
    internal suspend fun applyConfig(
        groupId: String,
        config: GroupExpenseConfig
    ) {
        val defaults = resolveDefaults(groupId, config)

        val memberIds = config.group.members
        val memberProfiles = getMemberProfilesUseCase(memberIds)
        val initialSplits = addExpenseSplitMapper.buildInitialSplits(
            memberIds = memberIds,
            shares = emptyList(),
            memberProfiles = memberProfiles
        )
        val currentUserId = authenticationService.currentUserId()
        val userSubunitOptions = filterSubunitsForCurrentUser(currentUserId, config)

        val isAiCapable = receiptExtractionService.capability() == ExtractionCapability.ON_DEVICE_AI
        val effectiveAiMode = resolveEffectiveAiMode(isAiCapable)
        val currentMillis = System.currentTimeMillis()
        val formattedDate = addExpenseUiMapper.formatExpenseDateForDisplay(currentMillis)

        _uiState.update {
            it.copy(
                isLoading = it.isEditMode && it.isLoading,
                isConfigLoaded = true,
                configLoadFailed = false,
                loadedGroupId = groupId,
                isAiCapable = isAiCapable,
                isAiModeActive = effectiveAiMode,
                currentStep = if (effectiveAiMode) AddExpenseStep.RECEIPT else AddExpenseStep.TITLE,
                groupName = config.group.name,
                currentUserId = currentUserId,
                expenseDateMillis = currentMillis,
                formattedExpenseDate = formattedDate,
                isExpenseDateValid = true,
                isExpenseDateModifiedByUser = false,
                groupCurrency = defaults.mappedGroupCurrency,
                availableCurrencies = defaults.mappedCurrencies,
                paymentMethods = defaults.reorderedPaymentMethods,
                fundingSources = defaults.mappedFundingSources,
                selectedFundingSource = defaults.defaultFundingSource,
                availableCategories = defaults.reorderedCategories,
                availablePaymentStatuses = defaults.mappedPaymentStatuses,
                selectedCurrency = defaults.initialCurrency,
                selectedPaymentMethod = defaults.defaultPaymentMethod,
                selectedCategory = defaults.defaultCategory,
                selectedPaymentStatus = defaults.defaultPaymentStatus,
                showExchangeRateSection = defaults.isForeign,
                exchangeRateLabel = defaults.exchangeRateLabel,
                groupAmountLabel = defaults.groupAmountLabel,
                availableSplitTypes = defaults.mappedSplitTypes,
                selectedSplitType = defaults.defaultSplitType,
                splits = initialSplits,
                memberIds = memberIds.toImmutableList(),
                contributionSubunitOptions = userSubunitOptions,
                error = null
            ).withStepClamped()
        }

        emitPostConfigActions(
            defaults.isForeign,
            defaults.defaultPaymentMethod,
            config,
            memberIds,
            memberProfiles
        )
    }

    /**
     * Resolves whether AI auto-fill mode should be active.
     *
     * AI mode is suppressed in edit mode because the expense already exists, so
     * re-uploading a receipt to extract fields would be nonsensical and would
     * cause a FOUC where the wizard briefly opens on the RECEIPT step before
     * the mapper restores TITLE as the current step.
     */
    private fun resolveEffectiveAiMode(isAiCapable: Boolean): Boolean =
        isAiCapable && !_uiState.value.isEditMode

    private suspend fun resolveDefaults(
        groupId: String,
        config: GroupExpenseConfig
    ): ConfigDefaults {
        val lastUsedCode = getGroupLastUsedCurrencyUseCase(groupId).firstOrNull()
        val recentPaymentMethodIds =
            getGroupLastUsedPaymentMethodUseCase(groupId).firstOrNull() ?: emptyList()
        val recentCategoryIds =
            getGroupLastUsedCategoryUseCase(groupId).firstOrNull() ?: emptyList()

        return resolveDefaultSelections(
            config,
            lastUsedCode,
            recentPaymentMethodIds,
            recentCategoryIds
        )
    }

    /**
     * Filters subunits from the config to only those where the current user is a member,
     * returning them as [SubunitOptionUiModel]s for the contribution scope picker.
     */
    internal fun filterSubunitsForCurrentUser(
        currentUserId: String?,
        config: GroupExpenseConfig
    ): ImmutableList<SubunitOptionUiModel> =
        config.subunits
            .filter { currentUserId in it.memberIds }
            .map { SubunitOptionUiModel(id = it.id, name = it.name) }
            .toImmutableList()

    /**
     * Maps domain config into UI option lists and resolves default selections
     * based on last-used preferences (MRU reordering).
     */
    // Sequential enum/config → UI-model mapping with preference resolution;
    // length is proportional to the number of option categories
    @Suppress("LongMethod")
    internal fun resolveDefaultSelections(
        config: GroupExpenseConfig,
        lastUsedCode: String?,
        recentPaymentMethodIds: List<String>,
        recentCategoryIds: List<String>
    ): ConfigDefaults {
        val mappedCurrencies = addExpenseOptionsMapper.mapCurrencies(config.availableCurrencies)
        val mappedGroupCurrency = addExpenseOptionsMapper.mapCurrency(config.groupCurrency)
        val mappedPaymentMethods = addExpenseOptionsMapper.mapPaymentMethods(
            PaymentMethod.entries
        )

        val reorderedPaymentMethods =
            reorderByRecent(mappedPaymentMethods, recentPaymentMethodIds) { it.id }
        val defaultPaymentMethod =
            recentPaymentMethodIds.firstOrNull()?.let { lastId ->
                reorderedPaymentMethods.find { it.id == lastId }
            } ?: reorderedPaymentMethods.firstOrNull()

        val mappedCategories = addExpenseOptionsMapper.mapCategories(ExpenseCategory.entries)
        val reorderedCategories =
            reorderByRecent(mappedCategories, recentCategoryIds) { it.id }
        val defaultCategory =
            recentCategoryIds.firstOrNull()?.let { lastId ->
                reorderedCategories.find { it.id == lastId }
            } ?: reorderedCategories.find { it.id == ExpenseCategory.OTHER.name }
                ?: reorderedCategories.lastOrNull()

        val mappedPaymentStatuses = addExpenseOptionsMapper.mapPaymentStatuses(
            PaymentStatus.entries
        )
        val defaultPaymentStatus =
            mappedPaymentStatuses.find { it.id == PaymentStatus.FINISHED.name }
                ?: mappedPaymentStatuses.firstOrNull()

        val initialCurrencyDomain =
            config.availableCurrencies.find { it.code == lastUsedCode }
                ?: config.groupCurrency
        val initialCurrency = addExpenseOptionsMapper.mapCurrency(initialCurrencyDomain)

        val isForeign = initialCurrency.code != mappedGroupCurrency.code
        val exchangeRateLabel = if (isForeign) {
            addExpenseOptionsMapper.buildExchangeRateLabel(mappedGroupCurrency, initialCurrency)
        } else {
            ""
        }
        val groupAmountLabel = addExpenseOptionsMapper.buildGroupAmountLabel(mappedGroupCurrency)

        val mappedSplitTypes = addExpenseOptionsMapper.mapSplitTypes(SplitType.entries)
        val defaultSplitType =
            mappedSplitTypes.find { it.id == SplitType.EQUAL.name }
                ?: mappedSplitTypes.firstOrNull()

        val mappedFundingSources = addExpenseOptionsMapper.mapFundingSources(PayerType.entries)
        val defaultFundingSource =
            mappedFundingSources.find { it.id == PayerType.GROUP.name }
                ?: mappedFundingSources.firstOrNull()

        return ConfigDefaults(
            mappedCurrencies = mappedCurrencies,
            mappedGroupCurrency = mappedGroupCurrency,
            reorderedPaymentMethods = reorderedPaymentMethods,
            defaultPaymentMethod = defaultPaymentMethod,
            mappedFundingSources = mappedFundingSources,
            defaultFundingSource = defaultFundingSource,
            reorderedCategories = reorderedCategories,
            defaultCategory = defaultCategory,
            mappedPaymentStatuses = mappedPaymentStatuses,
            defaultPaymentStatus = defaultPaymentStatus,
            initialCurrency = initialCurrency,
            isForeign = isForeign,
            exchangeRateLabel = exchangeRateLabel,
            groupAmountLabel = groupAmountLabel,
            mappedSplitTypes = mappedSplitTypes,
            defaultSplitType = defaultSplitType
        )
    }

    /**
     * Emits post-config actions via [postConfigCallback] — exchange rate fetching
     * and entity split initialization.
     */
    internal fun emitPostConfigActions(
        isForeign: Boolean,
        defaultPaymentMethod: PaymentMethodUiModel?,
        config: GroupExpenseConfig,
        memberIds: List<String>,
        memberProfiles: Map<String, User>
    ) {
        val isCash = defaultPaymentMethod?.let {
            try {
                PaymentMethod.fromString(it.id) == PaymentMethod.CASH
            } catch (_: IllegalArgumentException) {
                false
            }
        } ?: false

        if (isForeign) {
            if (isCash) {
                postConfigCallback?.invoke(PostConfigAction.FetchCashRate)
            } else {
                postConfigCallback?.invoke(PostConfigAction.FetchRate)
            }
        } else if (isCash) {
            // Same-currency CASH: fetch tranche preview for the "Funded from" section on AmountStep
            postConfigCallback?.invoke(PostConfigAction.FetchCashRate)
        }

        if (config.subunits.isNotEmpty()) {
            postConfigCallback?.invoke(
                PostConfigAction.InitEntitySplits(
                    memberIds,
                    config.subunits,
                    memberProfiles
                )
            )
        } else {
            postConfigCallback?.invoke(PostConfigAction.ClearEntitySplits)
        }
    }

    /**
     * Reorders a list so that items matching [recentIds] appear first (in MRU order),
     * followed by the remaining items in their original order.
     */
    private fun <T> reorderByRecent(
        items: ImmutableList<T>,
        recentIds: List<String>,
        idSelector: (T) -> String
    ): ImmutableList<T> {
        if (recentIds.isEmpty()) return items
        val recentIdSet = recentIds.toSet()
        val recent = recentIds.mapNotNull { id -> items.find { idSelector(it) == id } }
        val rest = items.filter { idSelector(it) !in recentIdSet }
        return (recent + rest).toImmutableList()
    }

    /**
     * Holds all mapped option lists and default selections resolved from
     * [GroupExpenseConfig] and user preferences.
     */
    internal data class ConfigDefaults(
        val mappedCurrencies: ImmutableList<CurrencyUiModel>,
        val mappedGroupCurrency: CurrencyUiModel,
        val reorderedPaymentMethods: ImmutableList<PaymentMethodUiModel>,
        val defaultPaymentMethod: PaymentMethodUiModel?,
        val mappedFundingSources: ImmutableList<FundingSourceUiModel>,
        val defaultFundingSource: FundingSourceUiModel?,
        val reorderedCategories: ImmutableList<CategoryUiModel>,
        val defaultCategory: CategoryUiModel?,
        val mappedPaymentStatuses: ImmutableList<PaymentStatusUiModel>,
        val defaultPaymentStatus: PaymentStatusUiModel?,
        val initialCurrency: CurrencyUiModel,
        val isForeign: Boolean,
        val exchangeRateLabel: String,
        val groupAmountLabel: String,
        val mappedSplitTypes: ImmutableList<SplitTypeUiModel>,
        val defaultSplitType: SplitTypeUiModel?
    )
}
