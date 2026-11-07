package es.pedrazamiguez.splittrip.features.withdrawal.presentation.viewmodel.state

import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.model.CurrencyUiModel
import es.pedrazamiguez.splittrip.core.designsystem.presentation.model.MemberOptionUiModel
import es.pedrazamiguez.splittrip.core.designsystem.presentation.model.SubunitOptionUiModel
import es.pedrazamiguez.splittrip.domain.converter.CurrencyConverter
import es.pedrazamiguez.splittrip.domain.enums.PayerType
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class AddCashWithdrawalUiState(
    val isLoading: Boolean = false,
    val isLoadingRate: Boolean = false,
    val isConfigLoaded: Boolean = false,
    val configLoadFailed: Boolean = false,
    val loadedGroupId: String? = null,
    val groupName: String? = null,

    // Currency
    val groupCurrency: CurrencyUiModel? = null,
    val selectedCurrency: CurrencyUiModel? = null,
    val availableCurrencies: ImmutableList<CurrencyUiModel> = persistentListOf(),

    // Inputs
    val withdrawalAmount: String = "",
    val deductedAmount: String = "",
    val displayExchangeRate: String = "1.0",
    val title: String = "",
    val notes: String = "",

    // Exchange rate section visibility
    val showExchangeRateSection: Boolean = false,
    val exchangeRateLabel: String = "",
    val deductedAmountLabel: String = "",
    /**
     * True when the exchange rate was served from an expired local cache
     * (the remote API was unreachable). Drives a warning banner in the
     * exchange rate section.
     */
    val isExchangeRateStale: Boolean = false,
    val isExchangeRateError: Boolean = false,
    /**
     * True when the fee exchange rate was served from an expired local cache.
     */
    val isFeeExchangeRateStale: Boolean = false,
    val isFeeExchangeRateError: Boolean = false,

    // ATM Fee (optional)
    val hasFee: Boolean = false,
    val feeAmount: String = "",
    val feeConvertedAmount: String = "",
    val feeCurrency: CurrencyUiModel? = null,
    val feeExchangeRate: String = "1.0",
    val feeExchangeRateLabel: String = "",
    val feeConvertedLabel: String = "",
    val showFeeExchangeRateSection: Boolean = false,
    val isFeeAmountValid: Boolean = true,

    // Withdrawal scope
    val withdrawalScope: PayerType = PayerType.GROUP,
    val selectedSubunitId: String? = null,
    val subunitOptions: ImmutableList<SubunitOptionUiModel> = persistentListOf(),

    // ── Member picker (impersonation) ─────────────────────────────────
    val groupMembers: ImmutableList<MemberOptionUiModel> = persistentListOf(),
    val selectedMemberId: String? = null,
    val selectedMemberDisplayName: String = "",

    // Validation
    val isAmountValid: Boolean = true,
    val isExchangeRateValid: Boolean = true,
    val isDeductedAmountValid: Boolean = true,
    val isFeeExchangeRateValid: Boolean = true,
    val isFeeConvertedAmountValid: Boolean = true,
    val error: UiText? = null,

    // ── Wizard ──────────────────────────────────────────────────────────
    val currentStep: CashWithdrawalStep = CashWithdrawalStep.AMOUNT,
    /**
     * When non-null, the user jumped from this step to REVIEW via "Skip to Review".
     * Pressing Back on REVIEW returns to this step instead of the previous sequential one.
     */
    val jumpedFromStep: CashWithdrawalStep? = null,
    val initialFormSnapshot: AddCashWithdrawalFormSnapshot? = null
) {
    val isDirty: Boolean
        get() = initialFormSnapshot != null && toFormSnapshot() != initialFormSnapshot

    fun toFormSnapshot() = AddCashWithdrawalFormSnapshot(
        withdrawalAmount = withdrawalAmount,
        title = title,
        notes = notes,
        selectedCurrency = selectedCurrency,
        displayExchangeRate = displayExchangeRate,
        hasFee = hasFee,
        feeAmount = feeAmount,
        feeCurrency = feeCurrency,
        feeExchangeRate = feeExchangeRate,
        withdrawalScope = withdrawalScope,
        selectedSubunitId = selectedSubunitId,
        selectedMemberId = selectedMemberId
    )

    val isReady: Boolean
        get() = isConfigLoaded && !configLoadFailed && !isLoading

    val isFormValid: Boolean
        get() {
            val hasAmount = withdrawalAmount.isNotBlank() && isAmountValid
            val isExchangeValid = !showExchangeRateSection || isExchangeSectionValid()
            val isFeeExchangeValid = !showFeeExchangeRateSection || isFeeExchangeSectionValid()
            return hasAmount && isExchangeValid && isFeeExchangeValid
        }

    private fun isExchangeSectionValid(): Boolean =
        isExchangeRateValid &&
            isDeductedAmountValid &&
            CurrencyConverter.isPositiveAmount(displayExchangeRate) &&
            CurrencyConverter.isPositiveAmount(deductedAmount)

    private fun isFeeExchangeSectionValid(): Boolean =
        isFeeExchangeRateValid &&
            isFeeConvertedAmountValid &&
            CurrencyConverter.isPositiveAmount(feeExchangeRate) &&
            CurrencyConverter.isPositiveAmount(feeConvertedAmount)

    // ── Wizard computed properties ──────────────────────────────────────

    /** Ordered list of steps that are currently applicable. */
    val applicableSteps: List<CashWithdrawalStep>
        get() = CashWithdrawalStep.applicableSteps(
            showExchangeRateSection = showExchangeRateSection,
            hasFee = hasFee,
            showFeeExchangeRateSection = showFeeExchangeRateSection
        )

    /** Zero-based index of the current step within [applicableSteps]. */
    val currentStepIndex: Int
        get() = applicableSteps.indexOf(currentStep).coerceAtLeast(0)

    /** Whether the wizard can navigate to the next step. */
    val canGoNext: Boolean
        get() = currentStepIndex < applicableSteps.lastIndex

    /** Whether the current step is the final review step. */
    val isOnReviewStep: Boolean
        get() = currentStep == CashWithdrawalStep.REVIEW

    /** Whether the current step is optional (can be skipped). */
    val isOnOptionalStep: Boolean
        get() = currentStep.isOptional

    /**
     * Zero-based indices of optional steps within [applicableSteps].
     *
     * Passed to [WizardStepIndicator] so optional steps can render with
     * a dashed-border visual treatment.
     */
    val optionalStepIndices: Set<Int>
        get() = applicableSteps
            .mapIndexedNotNull { index, step -> if (step.isOptional) index else null }
            .toSet()

    /**
     * Returns a copy with [currentStep] clamped to the nearest applicable step.
     *
     * Called after any state change that may shrink [applicableSteps] (e.g. switching
     * back to the group currency removes the EXCHANGE_RATE step, or disabling the ATM
     * fee removes ATM_FEE / FEE_EXCHANGE_RATE). Without clamping, [currentStep] could
     * point to a step that is no longer in [applicableSteps], causing the wizard content
     * and the indicator to disagree.
     */
    fun withStepClamped(): AddCashWithdrawalUiState {
        val steps = applicableSteps
        if (currentStep in steps) return this
        val clampedStep = steps.lastOrNull { it.ordinal < currentStep.ordinal } ?: steps.first()
        return copy(currentStep = clampedStep)
    }

    /** Whether the current step's fields pass validation (gates the "Next" button). */
    val isCurrentStepValid: Boolean
        get() = when (currentStep) {
            CashWithdrawalStep.AMOUNT ->
                withdrawalAmount.isNotBlank() && isAmountValid

            CashWithdrawalStep.EXCHANGE_RATE ->
                isExchangeRateValid &&
                    isDeductedAmountValid &&
                    CurrencyConverter.isPositiveAmount(displayExchangeRate) &&
                    CurrencyConverter.isPositiveAmount(deductedAmount)

            CashWithdrawalStep.SCOPE -> true // radio selection always has a value

            CashWithdrawalStep.DETAILS -> true // all fields are optional

            CashWithdrawalStep.ATM_FEE ->
                feeAmount.isNotBlank() && isFeeAmountValid

            CashWithdrawalStep.FEE_EXCHANGE_RATE ->
                isFeeExchangeRateValid &&
                    isFeeConvertedAmountValid &&
                    CurrencyConverter.isPositiveAmount(feeExchangeRate) &&
                    CurrencyConverter.isPositiveAmount(feeConvertedAmount)

            CashWithdrawalStep.REVIEW -> isFormValid
        }
}

data class AddCashWithdrawalFormSnapshot(
    val withdrawalAmount: String,
    val title: String,
    val notes: String,
    val selectedCurrency: CurrencyUiModel?,
    val displayExchangeRate: String,
    val hasFee: Boolean,
    val feeAmount: String,
    val feeCurrency: CurrencyUiModel?,
    val feeExchangeRate: String,
    val withdrawalScope: PayerType,
    val selectedSubunitId: String?,
    val selectedMemberId: String?
)
