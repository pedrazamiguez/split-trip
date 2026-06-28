package es.pedrazamiguez.splittrip.features.withdrawal.presentation.viewmodel.handler

import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.parseAmountToSmallestUnit
import es.pedrazamiguez.splittrip.core.designsystem.presentation.model.CurrencyUiModel
import es.pedrazamiguez.splittrip.domain.enums.AddOnMode
import es.pedrazamiguez.splittrip.domain.enums.AddOnType
import es.pedrazamiguez.splittrip.domain.enums.AddOnValueType
import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.domain.enums.PaymentMethod
import es.pedrazamiguez.splittrip.domain.model.AddOn
import es.pedrazamiguez.splittrip.domain.model.CashWithdrawal
import es.pedrazamiguez.splittrip.domain.service.CashWithdrawalValidationService
import es.pedrazamiguez.splittrip.domain.service.ExchangeRateCalculationService
import es.pedrazamiguez.splittrip.domain.usecase.balance.AddCashWithdrawalUseCase
import es.pedrazamiguez.splittrip.features.withdrawal.R
import es.pedrazamiguez.splittrip.features.withdrawal.presentation.viewmodel.action.AddCashWithdrawalUiAction
import es.pedrazamiguez.splittrip.features.withdrawal.presentation.viewmodel.state.AddCashWithdrawalUiState
import java.math.BigDecimal
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Handles withdrawal submission and validation:
 * [SubmitWithdrawal].
 */
class WithdrawalSubmitHandler(
    private val addCashWithdrawalUseCase: AddCashWithdrawalUseCase,
    private val cashWithdrawalValidationService: CashWithdrawalValidationService,
    private val exchangeRateCalculationService: ExchangeRateCalculationService
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

    fun submitWithdrawal(groupId: String?, onSuccess: () -> Unit) {
        if (groupId == null) return
        val state = _uiState.value
        val selectedCurrency = state.selectedCurrency ?: return
        val groupCurrency = state.groupCurrency ?: return

        val amountWithdrawn = parseAmountToSmallestUnit(
            state.withdrawalAmount,
            selectedCurrency.code
        )

        if (!validateInputs(state, amountWithdrawn, groupCurrency)) return

        val deductedBaseAmount = resolveDeductedAmount(state, amountWithdrawn, groupCurrency)
        val exchangeRate = resolveExchangeRate(state, amountWithdrawn, deductedBaseAmount)
        val addOns = buildFeeAddOn(state, groupCurrency)

        _uiState.update { it.copy(isLoading = true) }
        scope.launch {
            try {
                val withdrawal = CashWithdrawal(
                    groupId = groupId,
                    withdrawnBy = state.selectedMemberId ?: "",
                    withdrawalScope = state.withdrawalScope,
                    subunitId = if (state.withdrawalScope == PayerType.SUBUNIT) {
                        state.selectedSubunitId
                    } else {
                        null
                    },
                    amountWithdrawn = amountWithdrawn,
                    remainingAmount = amountWithdrawn,
                    currency = selectedCurrency.code,
                    deductedBaseAmount = deductedBaseAmount,
                    exchangeRate = exchangeRate,
                    addOns = addOns,
                    title = state.title.trim().ifBlank { null },
                    notes = state.notes.trim().ifBlank { null }
                )
                addCashWithdrawalUseCase(groupId, withdrawal).getOrThrow()
                onSuccess()
            } catch (e: es.pedrazamiguez.splittrip.domain.exception.GroupArchivedException) {
                _uiState.update { it.copy(isLoading = false) }
                _actions.emit(
                    AddCashWithdrawalUiAction.ShowError(
                        UiText.StringResource(
                            es.pedrazamiguez.splittrip.core.designsystem.R.string.group_error_archived
                        )
                    )
                )
            } catch (e: Exception) {
                Timber.e(e, "Failed to add cash withdrawal")
                _uiState.update { it.copy(isLoading = false) }
                _actions.emit(
                    AddCashWithdrawalUiAction.ShowError(
                        UiText.StringResource(R.string.withdrawal_cash_error)
                    )
                )
            }
        }
    }

    private fun validateInputs(
        state: AddCashWithdrawalUiState,
        amountWithdrawn: Long,
        groupCurrency: CurrencyUiModel
    ): Boolean {
        val amountValidation = cashWithdrawalValidationService.validateAmountWithdrawn(amountWithdrawn)
        if (amountValidation is CashWithdrawalValidationService.ValidationResult.Invalid) {
            _uiState.update { it.copy(isAmountValid = false) }
            return false
        }
        if (state.hasFee && state.feeAmount.isNotBlank()) {
            val feeCurrency = state.feeCurrency ?: groupCurrency
            val feeAmountCents = parseAmountToSmallestUnit(state.feeAmount, feeCurrency.code)
            if (feeAmountCents <= 0) {
                _uiState.update { it.copy(isFeeAmountValid = false) }
                return false
            }
        }
        return true
    }

    private fun resolveDeductedAmount(
        state: AddCashWithdrawalUiState,
        amountWithdrawn: Long,
        groupCurrency: CurrencyUiModel
    ): Long = if (state.showExchangeRateSection) {
        parseAmountToSmallestUnit(state.deductedAmount, groupCurrency.code)
    } else {
        amountWithdrawn
    }

    private fun resolveExchangeRate(
        state: AddCashWithdrawalUiState,
        amountWithdrawn: Long,
        deductedBaseAmount: Long
    ): BigDecimal = if (state.showExchangeRateSection) {
        exchangeRateCalculationService.calculateExchangeRate(
            amountWithdrawn = amountWithdrawn,
            deductedBaseAmount = deductedBaseAmount
        )
    } else {
        BigDecimal.ONE
    }

    /**
     * Builds the ATM fee add-on if user has enabled it and entered a valid amount.
     */
    private fun buildFeeAddOn(
        state: AddCashWithdrawalUiState,
        groupCurrency: CurrencyUiModel
    ): List<AddOn> {
        if (!state.hasFee || state.feeAmount.isBlank()) return emptyList()

        val feeCurrency = state.feeCurrency ?: groupCurrency
        val feeAmountCents = parseAmountToSmallestUnit(state.feeAmount, feeCurrency.code)
        if (feeAmountCents <= 0) return emptyList()

        val groupAmountCents = if (state.showFeeExchangeRateSection && state.feeConvertedAmount.isNotBlank()) {
            parseAmountToSmallestUnit(state.feeConvertedAmount, groupCurrency.code)
        } else {
            feeAmountCents
        }
        if (groupAmountCents <= 0) return emptyList()

        val feeExchangeRate = if (state.showFeeExchangeRateSection) {
            exchangeRateCalculationService.calculateExchangeRate(
                amountWithdrawn = feeAmountCents,
                deductedBaseAmount = groupAmountCents
            )
        } else {
            BigDecimal.ONE
        }

        return listOf(
            AddOn(
                id = UUID.randomUUID().toString(),
                type = AddOnType.FEE,
                mode = AddOnMode.ON_TOP,
                valueType = AddOnValueType.EXACT,
                amountCents = feeAmountCents,
                currency = feeCurrency.code,
                exchangeRate = feeExchangeRate,
                groupAmountCents = groupAmountCents,
                paymentMethod = PaymentMethod.OTHER,
                description = null
            )
        )
    }
}
