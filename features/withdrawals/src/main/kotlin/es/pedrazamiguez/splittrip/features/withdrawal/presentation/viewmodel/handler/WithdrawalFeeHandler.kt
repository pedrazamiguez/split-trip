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
 * Handles all ATM fee events:
 * [FeeToggled], [FeeAmountChanged], [FeeCurrencySelected],
 * [FeeExchangeRateChanged], [FeeConvertedAmountChanged].
 */
class WithdrawalFeeHandler(
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

    fun handleFeeToggled(hasFee: Boolean) {
        val state = _uiState.value
        val groupCurrency = state.groupCurrency ?: return

        if (hasFee) {
            val feeConvertedLabel = addCashWithdrawalUiMapper.buildFeeConvertedLabel(groupCurrency)
            _uiState.update {
                it.copy(
                    hasFee = true,
                    feeAmount = "",
                    feeCurrency = groupCurrency,
                    feeExchangeRate = "1.0",
                    feeConvertedAmount = "",
                    feeConvertedLabel = feeConvertedLabel,
                    showFeeExchangeRateSection = false,
                    isFeeAmountValid = true,
                    isFeeExchangeRateError = false
                ).withStepClamped()
            }
        } else {
            _uiState.update {
                it.copy(
                    hasFee = false,
                    feeAmount = "",
                    feeCurrency = null,
                    feeExchangeRate = "1.0",
                    feeConvertedAmount = "",
                    feeExchangeRateLabel = "",
                    feeConvertedLabel = "",
                    showFeeExchangeRateSection = false,
                    isFeeAmountValid = true,
                    isFeeExchangeRateError = false
                ).withStepClamped()
            }
        }
    }

    fun handleFeeAmountChanged(amount: String) {
        val isValid = amount.isValidDecimalInput()
        _uiState.update {
            it.copy(feeAmount = amount, isFeeAmountValid = isValid)
        }
        recalculateFeeConverted()
    }

    fun handleFeeCurrencySelected(currencyCode: String) {
        val state = _uiState.value
        val feeCurrencyModel = state.availableCurrencies
            .find { it.code == currencyCode } ?: return
        val groupCurrency = state.groupCurrency ?: return
        val isForeign = feeCurrencyModel.code != groupCurrency.code

        val feeExchangeRateLabel = if (isForeign) {
            addCashWithdrawalUiMapper.buildExchangeRateLabel(groupCurrency, feeCurrencyModel)
        } else {
            ""
        }

        _uiState.update {
            it.copy(
                feeCurrency = feeCurrencyModel,
                showFeeExchangeRateSection = isForeign,
                feeExchangeRateLabel = feeExchangeRateLabel,
                feeExchangeRate = if (isForeign) "" else "1.0",
                isFeeExchangeRateError = false
            ).withStepClamped()
        }

        if (isForeign) {
            fetchFeeRate(groupCurrency.code, feeCurrencyModel.code)
        }
        recalculateFeeConverted()
    }

    fun handleFeeExchangeRateChanged(rate: String) {
        _uiState.update { it.copy(feeExchangeRate = rate, isFeeExchangeRateError = false) }
        recalculateFeeConverted()
    }

    fun handleFeeConvertedAmountChanged(amount: String) {
        _uiState.update { it.copy(feeConvertedAmount = amount, isFeeExchangeRateError = false) }
        recalculateFeeRateFromConverted()
    }

    internal fun recalculateFeeConverted() {
        val state = _uiState.value
        if (!state.showFeeExchangeRateSection) {
            _uiState.update { it.copy(feeConvertedAmount = state.feeAmount) }
            return
        }
        if (state.feeExchangeRate.isBlank()) {
            _uiState.update { it.copy(feeConvertedAmount = "") }
            return
        }

        val sourceDecimalPlaces = state.feeCurrency?.decimalDigits ?: 2
        val targetDecimalPlaces = state.groupCurrency?.decimalDigits ?: 2
        val calculatedConverted = exchangeRateCalculationService.calculateGroupAmountFromDisplayRate(
            sourceAmountString = state.feeAmount,
            displayRateString = state.feeExchangeRate,
            sourceDecimalPlaces = sourceDecimalPlaces,
            targetDecimalPlaces = targetDecimalPlaces
        )
        val formatted = formattingHelper.formatForDisplay(
            internalValue = calculatedConverted,
            maxDecimalPlaces = targetDecimalPlaces,
            minDecimalPlaces = targetDecimalPlaces
        )
        _uiState.update { it.copy(feeConvertedAmount = formatted) }
    }

    private fun recalculateFeeRateFromConverted() {
        val state = _uiState.value
        if (!state.showFeeExchangeRateSection) return

        val sourceDecimalPlaces = state.feeCurrency?.decimalDigits ?: 2
        val impliedRate = exchangeRateCalculationService.calculateImpliedDisplayRateFromStrings(
            sourceAmountString = state.feeAmount,
            groupAmountString = state.feeConvertedAmount,
            sourceDecimalPlaces = sourceDecimalPlaces
        )
        val formatted = formattingHelper.formatRateForDisplay(impliedRate)
        _uiState.update { it.copy(feeExchangeRate = formatted) }
    }

    private fun fetchFeeRate(groupCurrencyCode: String, feeCurrencyCode: String) {
        scope.launch {
            try {
                val rateResult = getExchangeRateUseCase(
                    baseCurrencyCode = groupCurrencyCode,
                    targetCurrencyCode = feeCurrencyCode
                )
                _uiState.update { current ->
                    current.updateFeeRateResult(
                        groupCurrencyCode = groupCurrencyCode,
                        feeCurrencyCode = feeCurrencyCode,
                        rateResult = rateResult,
                        isError = rateResult == null
                    )
                }
                if (rateResult != null) {
                    recalculateFeeConverted()
                }
            } catch (e: Exception) {
                Timber.w(e, "Failed to fetch fee exchange rate")
                _uiState.update { current ->
                    current.updateFeeRateResult(
                        groupCurrencyCode = groupCurrencyCode,
                        feeCurrencyCode = feeCurrencyCode,
                        rateResult = null,
                        isError = true
                    )
                }
            }
        }
    }

    private fun AddCashWithdrawalUiState.updateFeeRateResult(
        groupCurrencyCode: String,
        feeCurrencyCode: String,
        rateResult: ExchangeRateWithStaleness?,
        isError: Boolean
    ): AddCashWithdrawalUiState {
        if (groupCurrency?.code != groupCurrencyCode ||
            feeCurrency?.code != feeCurrencyCode
        ) {
            return this
        }
        return copy(
            isFeeExchangeRateError = isError,
            feeExchangeRate = rateResult?.rate?.let { r ->
                formattingHelper.formatRateForDisplay(r.toPlainString())
            } ?: feeExchangeRate,
            isFeeExchangeRateStale = rateResult?.isStale ?: isFeeExchangeRateStale
        )
    }
}
