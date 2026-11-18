package es.pedrazamiguez.splittrip.features.withdrawal.presentation.viewmodel.handler

import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.core.designsystem.R as DesignSystemR
import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.isValidDecimalInput
import es.pedrazamiguez.splittrip.domain.converter.CurrencyConverter
import es.pedrazamiguez.splittrip.domain.result.ExchangeRateWithStaleness
import es.pedrazamiguez.splittrip.domain.service.ExchangeRateCalculationService
import es.pedrazamiguez.splittrip.domain.usecase.currency.GetExchangeRateUseCase
import es.pedrazamiguez.splittrip.features.withdrawal.presentation.mapper.AddCashWithdrawalUiMapper
import es.pedrazamiguez.splittrip.features.withdrawal.presentation.viewmodel.action.AddCashWithdrawalUiAction
import es.pedrazamiguez.splittrip.features.withdrawal.presentation.viewmodel.state.AddCashWithdrawalUiState
import kotlin.coroutines.cancellation.CancellationException
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
    private val addCashWithdrawalUiMapper: AddCashWithdrawalUiMapper
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
                displayExchangeRate = if (isForeign) "" else "1.0",
                deductedAmount = if (isForeign) it.deductedAmount else "",
                isExchangeRateValid = true
            ).withStepClamped()
        }

        if (isForeign) {
            fetchRate()
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
        val isValid = CurrencyConverter.isPositiveAmount(rate)
        _uiState.update {
            it.copy(
                displayExchangeRate = rate,
                isExchangeRateValid = isValid
            )
        }
        recalculateDeducted()
    }

    fun handleDeductedAmountChanged(amount: String) {
        val isValid = CurrencyConverter.isPositiveAmount(amount)
        _uiState.update {
            it.copy(
                deductedAmount = amount,
                isDeductedAmountValid = isValid
            )
        }
        recalculateRateFromDeducted()
    }

    /**
     * Forward calculation: from withdrawal amount + exchange rate → deducted amount.
     */
    fun recalculateDeducted() {
        val state = _uiState.value
        if (!state.showExchangeRateSection) return
        if (state.displayExchangeRate.isBlank()) {
            _uiState.update { it.copy(deductedAmount = "", isDeductedAmountValid = false) }
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
        val isDeductedValid = CurrencyConverter.isPositiveAmount(calculatedDeducted)
        _uiState.update {
            it.copy(
                deductedAmount = calculatedDeducted,
                isDeductedAmountValid = isDeductedValid
            )
        }
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
        val isRateValid = CurrencyConverter.isPositiveAmount(impliedRate)
        _uiState.update {
            it.copy(
                displayExchangeRate = impliedRate,
                isExchangeRateValid = isRateValid
            )
        }
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
                var applied = false
                _uiState.update { current ->
                    applied = current.groupCurrency?.code == requestedBaseCode &&
                        current.selectedCurrency?.code == requestedTargetCode
                    current.updateRateResult(
                        requestedBaseCode = requestedBaseCode,
                        requestedTargetCode = requestedTargetCode,
                        rateResult = rateResult
                    )
                }
                recalculateDeducted()
                if (applied) {
                    emitRateFetchOutcome(rateResult)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.w(e, "Failed to fetch exchange rate")
                var applied = false
                _uiState.update { current ->
                    applied = current.groupCurrency?.code == requestedBaseCode &&
                        current.selectedCurrency?.code == requestedTargetCode
                    current.updateRateResult(
                        requestedBaseCode = requestedBaseCode,
                        requestedTargetCode = requestedTargetCode,
                        rateResult = null
                    )
                }
                recalculateDeducted()
                if (applied) {
                    emitRateFetchOutcome(null)
                }
            }
        }
    }

    private suspend fun emitRateFetchOutcome(rateResult: ExchangeRateWithStaleness?) {
        when {
            rateResult == null -> _actions.emit(
                AddCashWithdrawalUiAction.ShowError(
                    UiText.StringResource(DesignSystemR.string.failed_rate_warning)
                )
            )

            rateResult.isStale -> _actions.emit(
                AddCashWithdrawalUiAction.ShowPill(
                    UiText.StringResource(DesignSystemR.string.stale_rate_warning)
                )
            )
        }
    }

    private fun AddCashWithdrawalUiState.updateRateResult(
        requestedBaseCode: String,
        requestedTargetCode: String,
        rateResult: ExchangeRateWithStaleness?
    ): AddCashWithdrawalUiState {
        if (groupCurrency?.code != requestedBaseCode ||
            selectedCurrency?.code != requestedTargetCode
        ) {
            return copy(isLoadingRate = false)
        }
        return copy(
            isLoadingRate = false,
            displayExchangeRate = rateResult?.rate?.stripTrailingZeros()?.toPlainString() ?: displayExchangeRate
        )
    }
}
