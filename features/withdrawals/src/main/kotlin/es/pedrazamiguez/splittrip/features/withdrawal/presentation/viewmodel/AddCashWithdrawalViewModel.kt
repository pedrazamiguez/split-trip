package es.pedrazamiguez.splittrip.features.withdrawal.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.WizardNavigator
import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.features.withdrawal.presentation.mapper.AddCashWithdrawalUiMapper
import es.pedrazamiguez.splittrip.features.withdrawal.presentation.viewmodel.action.AddCashWithdrawalUiAction
import es.pedrazamiguez.splittrip.features.withdrawal.presentation.viewmodel.event.AddCashWithdrawalUiEvent
import es.pedrazamiguez.splittrip.features.withdrawal.presentation.viewmodel.handler.WithdrawalConfigHandler
import es.pedrazamiguez.splittrip.features.withdrawal.presentation.viewmodel.handler.WithdrawalCurrencyHandler
import es.pedrazamiguez.splittrip.features.withdrawal.presentation.viewmodel.handler.WithdrawalFeeHandler
import es.pedrazamiguez.splittrip.features.withdrawal.presentation.viewmodel.handler.WithdrawalSubmitHandler
import es.pedrazamiguez.splittrip.features.withdrawal.presentation.viewmodel.state.AddCashWithdrawalUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Thin router ViewModel for the cash withdrawal wizard.
 *
 * All business logic is delegated to co-created handler classes:
 * - [WithdrawalConfigHandler] — group config loading
 * - [WithdrawalCurrencyHandler] — currency selection & exchange rate
 * - [WithdrawalFeeHandler] — ATM fee management
 * - [WithdrawalSubmitHandler] — validation & submission
 *
 * The ViewModel only manages wizard step navigation and member selection
 * inline (lightweight state updates).
 */
class AddCashWithdrawalViewModel(
    private val configHandler: WithdrawalConfigHandler,
    private val currencyHandler: WithdrawalCurrencyHandler,
    private val feeHandler: WithdrawalFeeHandler,
    private val submitHandler: WithdrawalSubmitHandler,
    private val addCashWithdrawalUiMapper: AddCashWithdrawalUiMapper
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddCashWithdrawalUiState())
    val uiState: StateFlow<AddCashWithdrawalUiState> = _uiState.asStateFlow()

    private val _actions = MutableSharedFlow<AddCashWithdrawalUiAction>()
    val actions: SharedFlow<AddCashWithdrawalUiAction> = _actions.asSharedFlow()

    private val wizardNavigator = WizardNavigator()

    init {
        configHandler.bind(_uiState, _actions, viewModelScope)
        currencyHandler.bind(_uiState, _actions, viewModelScope)
        feeHandler.bind(_uiState, _actions, viewModelScope)
        submitHandler.bind(_uiState, _actions, viewModelScope)
    }

    // Thin router — every branch is a single delegation;
    // complexity is proportional to event count, not logic
    @Suppress("CyclomaticComplexMethod")
    fun onEvent(event: AddCashWithdrawalUiEvent, onSuccess: () -> Unit = {}) {
        when (event) {
            // ── Config ──────────────────────────────────────────────────
            is AddCashWithdrawalUiEvent.LoadGroupConfig ->
                configHandler.loadGroupConfig(event.groupId)

            is AddCashWithdrawalUiEvent.RetryLoadConfig -> {
                _uiState.update { it.copy(configLoadFailed = false, error = null) }
                configHandler.loadGroupConfig(event.groupId, forceRefresh = true)
            }

            // ── Member Selection (Impersonation) ────────────────────────
            is AddCashWithdrawalUiEvent.MemberSelected ->
                handleMemberSelected(event.userId)

            // ── Currency & Exchange Rate ─────────────────────────────────
            is AddCashWithdrawalUiEvent.CurrencySelected ->
                currencyHandler.handleCurrencySelected(event.currencyCode)

            is AddCashWithdrawalUiEvent.WithdrawalAmountChanged ->
                currencyHandler.handleWithdrawalAmountChanged(event.amount)

            is AddCashWithdrawalUiEvent.DeductedAmountChanged ->
                currencyHandler.handleDeductedAmountChanged(event.amount)

            is AddCashWithdrawalUiEvent.ExchangeRateChanged ->
                currencyHandler.handleExchangeRateChanged(event.rate)

            // ── Details ──────────────────────────────────────────────────
            is AddCashWithdrawalUiEvent.WithdrawalScopeSelected ->
                _uiState.update {
                    it.copy(
                        withdrawalScope = event.scope,
                        selectedSubunitId = if (event.scope == PayerType.SUBUNIT) {
                            event.subunitId
                        } else {
                            null
                        }
                    )
                }

            is AddCashWithdrawalUiEvent.TitleChanged ->
                _uiState.update { it.copy(title = event.title) }

            is AddCashWithdrawalUiEvent.NotesChanged ->
                _uiState.update { it.copy(notes = event.notes) }

            // ── ATM Fee ──────────────────────────────────────────────────
            is AddCashWithdrawalUiEvent.FeeToggled ->
                feeHandler.handleFeeToggled(event.hasFee)

            is AddCashWithdrawalUiEvent.FeeAmountChanged ->
                feeHandler.handleFeeAmountChanged(event.amount)

            is AddCashWithdrawalUiEvent.FeeCurrencySelected ->
                feeHandler.handleFeeCurrencySelected(event.currencyCode)

            is AddCashWithdrawalUiEvent.FeeExchangeRateChanged ->
                feeHandler.handleFeeExchangeRateChanged(event.rate)

            is AddCashWithdrawalUiEvent.FeeConvertedAmountChanged ->
                feeHandler.handleFeeConvertedAmountChanged(event.amount)

            // ── Submit ───────────────────────────────────────────────────
            is AddCashWithdrawalUiEvent.SubmitWithdrawal ->
                submitHandler.submitWithdrawal(event.groupId, onSuccess)

            // ── Wizard Navigation ────────────────────────────────────────
            AddCashWithdrawalUiEvent.NextStep -> navigateNext()
            AddCashWithdrawalUiEvent.PreviousStep -> navigatePrevious()
            AddCashWithdrawalUiEvent.CloseWizard -> handleCloseWizard()
            AddCashWithdrawalUiEvent.JumpToReview -> navigateToReview()
            is AddCashWithdrawalUiEvent.JumpToStep -> navigateToStep(event.stepIndex)
            is AddCashWithdrawalUiEvent.ToggleStepInfoSheet ->
                _uiState.update { it.copy(isStepInfoSheetVisible = event.visible) }
        }
    }

    private fun handleMemberSelected(userId: String) {
        val subunitOptions = configHandler.filterSubunitsForMember(userId)

        _uiState.update {
            it.copy(
                selectedMemberId = userId,
                selectedMemberDisplayName = addCashWithdrawalUiMapper.resolveDisplayName(
                    userId,
                    it.groupMembers
                ),
                subunitOptions = subunitOptions,
                withdrawalScope = PayerType.GROUP,
                selectedSubunitId = null
            )
        }
    }

    private fun navigateNext() {
        val state = _uiState.value
        val next = wizardNavigator.navigateNext(state.currentStep, state.applicableSteps) ?: return
        _uiState.update { it.copy(currentStep = next) }
    }

    /**
     * Jumps directly to a previously completed step at [stepIndex].
     * Clears [AddCashWithdrawalUiState.jumpedFromStep] so that sequential Back navigation
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

            WizardNavigator.NavigationResult.ExitWizard -> {
                if (_uiState.value.isDirty) {
                    viewModelScope.launch { _actions.emit(AddCashWithdrawalUiAction.RequestExitConfirmation) }
                } else {
                    viewModelScope.launch { _actions.emit(AddCashWithdrawalUiAction.NavigateBack) }
                }
            }
        }
    }

    private fun handleCloseWizard() {
        if (_uiState.value.isDirty) {
            viewModelScope.launch { _actions.emit(AddCashWithdrawalUiAction.RequestExitConfirmation) }
        } else {
            viewModelScope.launch { _actions.emit(AddCashWithdrawalUiAction.NavigateBack) }
        }
    }
}
