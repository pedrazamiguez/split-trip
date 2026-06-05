package es.pedrazamiguez.splittrip.features.withdrawal.presentation.viewmodel.handler

import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.FormattingHelper
import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.isValidDecimalInput
import es.pedrazamiguez.splittrip.domain.result.ExchangeRateWithStaleness
import es.pedrazamiguez.splittrip.domain.service.ExchangeRateCalculationService
import es.pedrazamiguez.splittrip.domain.usecase.currency.GetExchangeRateUseCase
import es.pedrazamiguez.splittrip.features.withdrawal.presentation.mapper.AddCashWithdrawalUiMapper
import es.pedrazamiguez.splittrip.features.withdrawal.presentation.viewmodel.action.AddCashWithdrawalUiAction
import es.pedrazamiguez.splittrip.features.withdrawal.presentation.viewmodel.state.AddCashWithdrawalUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Handles currency selection and exchange rate events:
 * [CurrencySelected], [WithdrawalAmountChanged], [ExchangeRateChanged], [DeductedAmountChanged].
 *
 * Also exposes [recalculateDeducted] for cross-handler calls.
 */
class WithdrawalCurrencyHandler(
    private val getExchangeRateUseCase: GetExchangeRateUseCase,
    private val exchangeRateCalculationService: ExchangeRateCalculationService,
    private val addCashWithdrawalUiMapper: AddCashWithdrawalUiMapper,
    private val formattingHelper: FormattingHelper
) : AddCashWithdrawalEventHandler {

    private lateinit var _uiState: MutableStateFlow<AddCashWithdrawalUiState>
    private lateinit var _actions: MutableSharedFlow<AddCashWithdrawalUiAction>
    private lateinit var scope: CoroutineScope

    override fun bind(
        stateFlow: MutableStateFlow<AddCashWithdrawalUiState>,
        actionsFlow: MutableSharedFlow<AddCashWithdrawalUiAction>,
        scope: CoroutineScope
    ) {
        _uiState = stateFlow
        _actions = actionsFlow
        this.scope = scope
    }

    fun handleCurrencySelected(currencyCode: String) {
        val currentState = _uiState.value
        val selectedUiModel = currentState.availableCurrencies
            .find { it.code == currencyCode } ?: return
        val isForeign = selectedUiModel.code != currentState.groupCurrency?.code

        val exchangeRateLabel = if (isForeign && currentState.groupCurrency != null) {
            addCashWithdrawalUiMapper.buildExchangeRateLabel(currentState.groupCurrency, selectedUiModel)
        } else {
            ""
        }

        _uiState.update {
            it.copy(
                selectedCurrency = selectedUiModel,
                showExchangeRateSection = isForeign,
                exchangeRateLabel = exchangeRateLabel,
                isExchangeRateError = false
            ).withStepClamped()
        }

        if (isForeign) {
            _uiState.update { it.copy(displayExchangeRate = "") }
            fetchRate()
        } else {
            _uiState.update { it.copy(displayExchangeRate = "1.0", deductedAmount = "") }
        }
        recalculateDeducted()
    }

    fun handleWithdrawalAmountChanged(amount: String) {
        val isValid = amount.isValidDecimalInput()
        _uiState.update {
            it.copy(withdrawalAmount = amount, isAmountValid = isValid, error = null)
        }
        recalculateDeducted()
    }

    fun handleExchangeRateChanged(rate: String) {
        _uiState.update { it.copy(displayExchangeRate = rate, isExchangeRateError = false) }
        recalculateDeducted()
    }

    fun handleDeductedAmountChanged(amount: String) {
        _uiState.update { it.copy(deductedAmount = amount, isExchangeRateError = false) }
        recalculateRateFromDeducted()
    }

    /**
     * Forward calculation: from withdrawal amount + exchange rate → deducted amount.
     */
    fun recalculateDeducted() {
        val state = _uiState.value
        if (!state.showExchangeRateSection) return
        if (state.displayExchangeRate.isBlank()) {
            _uiState.update { it.copy(deductedAmount = "") }
            return
        }

        val sourceDecimalPlaces = state.selectedCurrency?.decimalDigits ?: 2
        val targetDecimalPlaces = state.groupCurrency?.decimalDigits ?: 2
        val calculatedDeducted = exchangeRateCalculationService.calculateGroupAmountFromDisplayRate(
            sourceAmountString = state.withdrawalAmount,
            displayRateString = state.displayExchangeRate,
            sourceDecimalPlaces = sourceDecimalPlaces,
            targetDecimalPlaces = targetDecimalPlaces
        )
        val formatted = formattingHelper.formatForDisplay(
            internalValue = calculatedDeducted,
            maxDecimalPlaces = targetDecimalPlaces,
            minDecimalPlaces = targetDecimalPlaces
        )
        _uiState.update { it.copy(deductedAmount = formatted) }
    }

    /**
     * Reverse calculation: from withdrawal amount + deducted amount → implied exchange rate.
     */
    private fun recalculateRateFromDeducted() {
        val state = _uiState.value
        if (!state.showExchangeRateSection) return

        val sourceDecimalPlaces = state.selectedCurrency?.decimalDigits ?: 2
        val impliedRate = exchangeRateCalculationService.calculateImpliedDisplayRateFromStrings(
            sourceAmountString = state.withdrawalAmount,
            groupAmountString = state.deductedAmount,
            sourceDecimalPlaces = sourceDecimalPlaces
        )
        val formatted = formattingHelper.formatRateForDisplay(impliedRate)
        _uiState.update { it.copy(displayExchangeRate = formatted) }
    }

    private fun fetchRate() {
        val state = _uiState.value
        val groupCurrency = state.groupCurrency
        val selectedCurrency = state.selectedCurrency

        if (groupCurrency == null ||
            selectedCurrency == null ||
            groupCurrency.code == selectedCurrency.code
        ) {
            return
        }

        val requestedBaseCode = groupCurrency.code
        val requestedTargetCode = selectedCurrency.code

        scope.launch {
            _uiState.update { it.copy(isLoadingRate = true) }
            try {
                val rateResult = getExchangeRateUseCase(
                    baseCurrencyCode = requestedBaseCode,
                    targetCurrencyCode = requestedTargetCode
                )
                _uiState.update { current ->
                    current.updateRateResult(
                        requestedBaseCode = requestedBaseCode,
                        requestedTargetCode = requestedTargetCode,
                        rateResult = rateResult,
                        isError = rateResult == null
                    )
                }
                if (rateResult != null) recalculateDeducted()
            } catch (e: Exception) {
                Timber.w(e, "Failed to fetch exchange rate")
                _uiState.update { current ->
                    current.updateRateResult(
                        requestedBaseCode = requestedBaseCode,
                        requestedTargetCode = requestedTargetCode,
                        rateResult = null,
                        isError = true
                    )
                }
            }
        }
    }

    private fun AddCashWithdrawalUiState.updateRateResult(
        requestedBaseCode: String,
        requestedTargetCode: String,
        rateResult: ExchangeRateWithStaleness?,
        isError: Boolean
    ): AddCashWithdrawalUiState {
        if (groupCurrency?.code != requestedBaseCode ||
            selectedCurrency?.code != requestedTargetCode
        ) {
            return copy(isLoadingRate = false)
        }
        return copy(
            isLoadingRate = false,
            isExchangeRateError = isError,
            displayExchangeRate = rateResult?.rate?.let { r ->
                formattingHelper.formatRateForDisplay(r.toPlainString())
            } ?: displayExchangeRate,
            isExchangeRateStale = rateResult?.isStale ?: isExchangeRateStale
        )
    }
}
