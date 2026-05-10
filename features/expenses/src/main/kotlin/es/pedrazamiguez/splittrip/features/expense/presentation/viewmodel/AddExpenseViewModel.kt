package es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.WizardNavigator
import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.action.AddExpenseUiAction
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.event.AddExpenseUiEvent
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.handler.AddOnEventHandler
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.handler.ConfigEventHandler
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.handler.CurrencyEventHandler
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.handler.FormEventHandler
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.handler.FormPostAction
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.handler.PostConfigAction
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.handler.SplitEventHandler
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.handler.SubmitEventHandler
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.handler.SubunitSplitEventHandler
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.AddExpenseUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AddExpenseViewModel(
    private val configEventHandler: ConfigEventHandler,
    private val currencyEventHandler: CurrencyEventHandler,
    private val splitEventHandler: SplitEventHandler,
    private val subunitSplitEventHandler: SubunitSplitEventHandler,
    private val addOnEventHandler: AddOnEventHandler,
    private val submitEventHandler: SubmitEventHandler,
    private val formEventHandler: FormEventHandler
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddExpenseUiState())
    val uiState: StateFlow<AddExpenseUiState> = _uiState.asStateFlow()

    private val _actions = MutableSharedFlow<AddExpenseUiAction>()
    val actions: SharedFlow<AddExpenseUiAction> = _actions.asSharedFlow()

    private val wizardNavigator = WizardNavigator()

    init {
        // Bind all handlers to the shared state and actions flows
        configEventHandler.bind(_uiState, _actions, viewModelScope)
        currencyEventHandler.bind(_uiState, _actions, viewModelScope)
        splitEventHandler.bind(_uiState, _actions, viewModelScope)
        subunitSplitEventHandler.bind(_uiState, _actions, viewModelScope)
        addOnEventHandler.bind(_uiState, _actions, viewModelScope)
        submitEventHandler.bind(_uiState, _actions, viewModelScope)
        formEventHandler.bind(_uiState, _actions, viewModelScope)

        // Wire post-config callback: ViewModel routes cross-handler actions
        configEventHandler.setPostConfigCallback { action ->
            when (action) {
                is PostConfigAction.FetchRate ->
                    currencyEventHandler.fetchRate()

                is PostConfigAction.FetchCashRate ->
                    currencyEventHandler.fetchPoolsIfNeeded()

                is PostConfigAction.InitEntitySplits ->
                    subunitSplitEventHandler.initEntitySplits(
                        action.memberIds,
                        action.subunits,
                        action.memberProfiles
                    )

                is PostConfigAction.ClearEntitySplits ->
                    subunitSplitEventHandler.clearEntitySplits()
            }
        }

        // Wire post-form callback: ViewModel routes cross-handler actions
        formEventHandler.setFormPostCallback { action ->
            when (action) {
                is FormPostAction.RecalculateAfterAmount ->
                    recalculateAfterAmountChange(action.isExchangeRateLocked, action.isCash)

                is FormPostAction.PaymentMethodChanged ->
                    currencyEventHandler.handlePaymentMethodChanged(
                        action.isCash,
                        action.isGroupPocket
                    )

                is FormPostAction.FundingSourceChanged ->
                    currencyEventHandler.handleFundingSourceChanged(action.isGroupPocket)
            }
        }

        // Wire pool-resolved callback: auto-selection path triggers split smart-default.
        // Explicit user selection is handled directly in the WithdrawalPoolSelected branch of onEvent.
        currencyEventHandler.setOnPoolResolvedCallback { poolScope, poolOwnerId ->
            splitEventHandler.applyPersonalPoolSplitDefault(
                poolScope = poolScope,
                poolOwnerId = poolOwnerId,
                currentUserId = _uiState.value.currentUserId
            )
            if (poolScope == PayerType.SUBUNIT) {
                subunitSplitEventHandler.applySubunitPoolDefault(poolOwnerId)
            } else {
                subunitSplitEventHandler.disableSubunitMode()
            }
        }
    }

    // Thin router — every branch is a single delegation;
    // complexity is proportional to event count, not logic
    @Suppress("LongMethod", "CyclomaticComplexMethod")
    fun onEvent(event: AddExpenseUiEvent, onAddExpenseSuccess: () -> Unit = {}) {
        when (event) {
            // ── Config ──────────────────────────────────────────────────
            is AddExpenseUiEvent.LoadGroupConfig ->
                configEventHandler.loadGroupConfig(event.groupId)

            is AddExpenseUiEvent.RetryLoadConfig -> {
                _uiState.update { it.copy(configLoadFailed = false, error = null) }
                configEventHandler.loadGroupConfig(event.groupId, forceRefresh = true)
            }

            // ── Currency & Exchange Rate ────────────────────────────────
            is AddExpenseUiEvent.CurrencySelected ->
                currencyEventHandler.handleCurrencySelected(event.currencyCode) {
                    splitEventHandler.recalculateSplits()
                    subunitSplitEventHandler.recalculateEntitySplits()
                    addOnEventHandler.recalculateEffectiveTotal()
                }

            is AddExpenseUiEvent.ExchangeRateChanged ->
                currencyEventHandler.handleExchangeRateChanged(event.rate)

            is AddExpenseUiEvent.GroupAmountChanged ->
                currencyEventHandler.handleGroupAmountChanged(event.amount)

            is AddExpenseUiEvent.WithdrawalPoolSelected -> {
                currencyEventHandler.handleWithdrawalPoolSelected(event.scope, event.scopeOwnerId)
                splitEventHandler.applyPersonalPoolSplitDefault(
                    poolScope = event.scope,
                    poolOwnerId = event.scopeOwnerId,
                    currentUserId = _uiState.value.currentUserId
                )
                if (event.scope == PayerType.SUBUNIT) {
                    subunitSplitEventHandler.applySubunitPoolDefault(event.scopeOwnerId)
                } else {
                    subunitSplitEventHandler.disableSubunitMode()
                }
            }

            // ── Splits ──────────────────────────────────────────────────
            is AddExpenseUiEvent.SplitTypeChanged -> {
                splitEventHandler.handleSplitTypeChanged(event.splitTypeId)
                subunitSplitEventHandler.recalculateEntitySplits()
            }

            is AddExpenseUiEvent.SplitAmountChanged ->
                splitEventHandler.handleExactAmountChanged(event.userId, event.amount)

            is AddExpenseUiEvent.SplitPercentageChanged ->
                splitEventHandler.handlePercentageChanged(event.userId, event.percentage)

            is AddExpenseUiEvent.SplitExcludedToggled ->
                splitEventHandler.handleSplitExcludedToggled(event.userId)

            is AddExpenseUiEvent.SplitShareLockToggled ->
                splitEventHandler.handleShareLockToggled(event.userId)

            // ── Subunit splits ────────────────────────────────────────────
            is AddExpenseUiEvent.SubunitModeToggled -> {
                subunitSplitEventHandler.handleSubunitModeToggled()
                // Mode switch changes which splits are visible → recheck warning.
                splitEventHandler.recomputePersonalCashWarning()
            }

            is AddExpenseUiEvent.EntityAccordionToggled ->
                subunitSplitEventHandler.handleAccordionToggled(event.entityId)

            is AddExpenseUiEvent.EntitySplitExcludedToggled -> {
                subunitSplitEventHandler.handleEntityExcludedToggled(event.entityId)
                // Entity inclusion change may bring out-of-scope entities into the split.
                splitEventHandler.recomputePersonalCashWarning()
            }

            is AddExpenseUiEvent.EntitySplitAmountChanged ->
                subunitSplitEventHandler.handleEntityAmountChanged(event.entityId, event.amount)

            is AddExpenseUiEvent.EntitySplitPercentageChanged ->
                subunitSplitEventHandler.handleEntityPercentageChanged(event.entityId, event.percentage)

            is AddExpenseUiEvent.EntityShareLockToggled ->
                subunitSplitEventHandler.handleEntityShareLockToggled(event.entityId)

            is AddExpenseUiEvent.IntraSubunitSplitTypeChanged ->
                subunitSplitEventHandler.handleIntraSubunitSplitTypeChanged(event.subunitId, event.splitTypeId)

            is AddExpenseUiEvent.IntraSubunitAmountChanged ->
                subunitSplitEventHandler.handleIntraSubunitAmountChanged(event.subunitId, event.userId, event.amount)

            is AddExpenseUiEvent.IntraSubunitPercentageChanged ->
                subunitSplitEventHandler.handleIntraSubunitPercentageChanged(
                    event.subunitId,
                    event.userId,
                    event.percentage
                )

            is AddExpenseUiEvent.IntraSubunitShareLockToggled ->
                subunitSplitEventHandler.handleIntraSubunitShareLockToggled(event.subunitId, event.userId)

            // ── Submission ──────────────────────────────────────────────
            is AddExpenseUiEvent.SubmitAddExpense ->
                submitEventHandler.submitExpense(event.groupId, onAddExpenseSuccess)

            // ── Simple form field updates (delegated to FormEventHandler) ──
            is AddExpenseUiEvent.TitleChanged ->
                formEventHandler.handleTitleChanged(event.title)

            is AddExpenseUiEvent.SourceAmountChanged ->
                formEventHandler.handleSourceAmountChanged(event.amount)

            is AddExpenseUiEvent.PaymentMethodSelected ->
                formEventHandler.handlePaymentMethodSelected(event.methodId)

            is AddExpenseUiEvent.FundingSourceSelected ->
                formEventHandler.handleFundingSourceSelected(event.fundingSourceId)

            is AddExpenseUiEvent.ContributionScopeSelected ->
                formEventHandler.handleContributionScopeSelected(event.scope, event.subunitId)

            is AddExpenseUiEvent.CategorySelected ->
                formEventHandler.handleCategorySelected(event.categoryId)

            is AddExpenseUiEvent.VendorChanged ->
                formEventHandler.handleVendorChanged(event.vendor)

            is AddExpenseUiEvent.NotesChanged ->
                formEventHandler.handleNotesChanged(event.notes)

            is AddExpenseUiEvent.PaymentStatusSelected ->
                formEventHandler.handlePaymentStatusSelected(event.statusId)

            is AddExpenseUiEvent.DueDateSelected ->
                formEventHandler.handleDueDateSelected(event.dateMillis)

            is AddExpenseUiEvent.ReceiptImageSelected ->
                formEventHandler.handleReceiptImageChanged(event.uri)

            is AddExpenseUiEvent.RemoveReceiptImage ->
                formEventHandler.handleReceiptImageChanged(null)

            // ── Add-Ons ─────────────────────────────────────────────────
            is AddExpenseUiEvent.AddOnAdded ->
                addOnEventHandler.handleAddOnAdded(event.type)

            is AddExpenseUiEvent.AddOnRemoved ->
                addOnEventHandler.handleAddOnRemoved(event.addOnId)

            is AddExpenseUiEvent.AddOnTypeChanged ->
                addOnEventHandler.handleTypeChanged(event.addOnId, event.type)

            is AddExpenseUiEvent.AddOnModeChanged ->
                addOnEventHandler.handleModeChanged(event.addOnId, event.mode)

            is AddExpenseUiEvent.AddOnValueTypeChanged ->
                addOnEventHandler.handleValueTypeChanged(
                    event.addOnId,
                    event.valueType
                )

            is AddExpenseUiEvent.AddOnAmountChanged ->
                addOnEventHandler.handleAmountChanged(event.addOnId, event.amount)

            is AddExpenseUiEvent.AddOnCurrencySelected ->
                addOnEventHandler.handleCurrencySelected(
                    event.addOnId,
                    event.currencyCode
                )

            is AddExpenseUiEvent.AddOnPaymentMethodSelected ->
                addOnEventHandler.handlePaymentMethodSelected(
                    event.addOnId,
                    event.methodId
                )

            is AddExpenseUiEvent.AddOnDescriptionChanged ->
                addOnEventHandler.handleDescriptionChanged(
                    event.addOnId,
                    event.description
                )

            is AddExpenseUiEvent.AddOnExchangeRateChanged ->
                addOnEventHandler.handleExchangeRateChanged(
                    event.addOnId,
                    event.rate
                )

            is AddExpenseUiEvent.AddOnGroupAmountChanged ->
                addOnEventHandler.handleGroupAmountChanged(
                    event.addOnId,
                    event.amount
                )

            is AddExpenseUiEvent.AddOnsSectionToggled ->
                addOnEventHandler.handleSectionToggled()

            // ── Conflict Resolution ──────────────────────────────────────
            is AddExpenseUiEvent.ResolutionAmountSelected ->
                formEventHandler.handleSourceAmountChanged(event.amount)

            // ── Wizard Navigation ────────────────────────────────────────
            AddExpenseUiEvent.NextStep -> navigateNext()
            AddExpenseUiEvent.PreviousStep -> navigatePrevious()
            AddExpenseUiEvent.JumpToReview -> navigateToReview()
            is AddExpenseUiEvent.JumpToStep -> navigateToStep(event.stepIndex)
        }
    }

    /**
     * Orchestrates cross-handler recalculations after a source amount change.
     * Called via [FormPostAction.RecalculateAfterAmount].
     *
     * For foreign CASH ([isExchangeRateLocked] = true), the FIFO-based cash rate fetch
     * (which also updates tranche previews) replaces the forward calculation.
     * For same-currency GROUP-pocket CASH, both [recalculateForward] and [recalculateCashForward]
     * must be called so the "Funded from" section stays current.
     * USER/SUBUNIT pocket CASH is excluded — the rate is user-editable and the ATM pool
     * is not queried for those pockets.
     */
    private fun recalculateAfterAmountChange(isExchangeRateLocked: Boolean, isCash: Boolean) {
        if (isExchangeRateLocked) {
            currencyEventHandler.recalculateCashForward()
        } else {
            currencyEventHandler.recalculateForward()
            // Same-currency GROUP-pocket CASH: refresh tranche previews on AmountStep.
            // Gate on payment method + GROUP pocket + same currency (not exchange-rate step) to
            // avoid querying the ATM pool for USER/SUBUNIT pocket CASH where the rate is editable.
            // Cannot guard on cashTranchePreviews.isNotEmpty() because the list starts empty and
            // the first positive-amount entry must still trigger the initial fetch.
            if (isCash) {
                val state = _uiState.value
                val isGroupPocket = state.selectedFundingSource?.id == PayerType.GROUP.name
                val isSameCurrency = !state.showExchangeRateSection
                if (isGroupPocket && isSameCurrency) {
                    currencyEventHandler.recalculateCashForward()
                }
            }
        }
        splitEventHandler.recalculateSplits()
        subunitSplitEventHandler.recalculateEntitySplits()
        addOnEventHandler.recalculateEffectiveTotal()
    }

    /**
     * Re-runs [PreviewCashExchangeRateUseCase] to refresh the tranche preview panel
     * with the latest Room data. Called by the Feature after a [AddExpenseUiAction.ShowCashConflictError]
     * is received — i.e. when a concurrent cash expense by another group member caused
     * [InsufficientCashException] at save time.
     */
    fun refreshCashPreview() {
        currencyEventHandler.fetchCashRate()
    }

    private fun navigateNext() {
        val state = _uiState.value
        val next = wizardNavigator.navigateNext(state.currentStep, state.applicableSteps) ?: return
        _uiState.update { it.copy(currentStep = next) }
    }

    /**
     * Jumps directly to a previously completed step at [stepIndex].
     * Clears [AddExpenseUiState.jumpedFromStep] so that sequential Back navigation
     * is not misrouted after the jump.
     */
    private fun navigateToStep(stepIndex: Int) {
        val state = _uiState.value
        val target = wizardNavigator.jumpToStep(state.currentStep, stepIndex, state.applicableSteps) ?: return
        _uiState.update { it.copy(currentStep = target, jumpedFromStep = null) }
    }

    /**
     * Jumps directly from the current optional step to the REVIEW step.
     * Records the departure step so [navigatePrevious] can return to it.
     */
    private fun navigateToReview() {
        val state = _uiState.value
        val reviewStep = wizardNavigator.navigateToReview(state.currentStep, state.applicableSteps) ?: return
        _uiState.update { it.copy(currentStep = reviewStep, jumpedFromStep = state.currentStep) }
    }

    private fun navigatePrevious() {
        val state = _uiState.value
        when (
            val result = wizardNavigator.navigatePrevious(
                state.currentStep,
                state.jumpedFromStep,
                state.applicableSteps
            )
        ) {
            is WizardNavigator.NavigationResult.WithStep ->
                _uiState.update { it.copy(currentStep = result.step, jumpedFromStep = null) }

            WizardNavigator.NavigationResult.ExitWizard ->
                // On first step — signal the Feature to pop the back stack
                viewModelScope.launch { _actions.emit(AddExpenseUiAction.NavigateBack) }
        }
    }
}
