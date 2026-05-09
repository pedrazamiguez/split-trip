package es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state

import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.model.CurrencyUiModel
import es.pedrazamiguez.splittrip.core.designsystem.presentation.model.SubunitOptionUiModel
import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.features.expense.presentation.model.AddOnUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.model.CashTranchePreviewUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.model.CategoryUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.model.FundingSourceUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.model.PaymentMethodUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.model.PaymentStatusUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.model.SplitTypeUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.model.SplitUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.model.WithdrawalPoolOptionUiModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class AddExpenseUiState(
    val isLoading: Boolean = false,
    val isLoadingRate: Boolean = false,
    val isConfigLoaded: Boolean = false,
    val configLoadFailed: Boolean = false,
    val loadedGroupId: String? = null,
    val groupName: String? = null,
    val currentUserId: String? = null,

    // Inputs
    val expenseTitle: String = "",
    val sourceAmount: String = "",
    val vendor: String = "",
    val notes: String = "",

    // Selection
    val selectedCurrency: CurrencyUiModel? = null,
    val selectedPaymentMethod: PaymentMethodUiModel? = null,
    val selectedFundingSource: FundingSourceUiModel? = null,
    /**
     * Contextual hint displayed when "My Money" funding source is selected.
     * Explains that the user will be reimbursed for others' shares.
     */
    val fundingSourceHint: UiText? = null,
    val selectedCategory: CategoryUiModel? = null,
    val selectedPaymentStatus: PaymentStatusUiModel? = null,

    // Calculated / Display Data
    val groupCurrency: CurrencyUiModel? = null,
    /**
     * User-friendly exchange rate displayed in the UI.
     * Represents "1 [GroupCurrency] = X [SourceCurrency]" (e.g., "1 EUR = 37 THB").
     * This is the INVERSE of the internal calculation rate.
     */
    val displayExchangeRate: String = "1.0",
    val calculatedGroupAmount: String = "", // "Cost in EUR"
    val showExchangeRateSection: Boolean = false,
    /**
     * True when the exchange rate is determined by ATM withdrawal rates (CASH payment)
     * and should not be editable by the user.
     */
    val isExchangeRateLocked: Boolean = false,
    /**
     * Informational message explaining why the rate is locked.
     * Shown in the exchange rate section when [isExchangeRateLocked] is true.
     */
    val exchangeRateLockedHint: UiText? = null,
    /**
     * Snapshot of [displayExchangeRate] taken just before switching to CASH payment.
     * Restored when the user switches back to a non-CASH method so the custom
     * (or previously fetched) rate is not lost.
     * Cleared when the selected currency changes because the saved rate would
     * belong to a different currency pair.
     */
    val preCashExchangeRate: String? = null,
    /**
     * True when the entered source amount exceeds the available cash withdrawals.
     * Drives warning styling in the exchange rate hint.
     */
    val isInsufficientCash: Boolean = false,
    /**
     * Ordered list of ATM withdrawal tranches that will fund this cash expense,
     * derived from a simulated FIFO run in [PreviewCashExchangeRateUseCase].
     *
     * Non-empty when [selectedPaymentMethod] is CASH and the user has entered a positive
     * source amount that triggered a FIFO simulation. This includes ATM-backed CASH flows
     * where [isExchangeRateLocked] is true and same-currency CASH flows where tranche
     * previews are shown even though the rate is not locked.
     * Cleared on currency change, payment method change, or insufficient cash.
     */
    val cashTranchePreviews: ImmutableList<CashTranchePreviewUiModel> = persistentListOf(),
    /**
     * Available cash withdrawal pools for the current expense's scope and currency.
     *
     * Populated when the payment method is CASH and multiple pools have available funds.
     * This includes GROUP-scoped expenses when the user also holds personal (USER-scoped) cash.
     * Contains one entry per pool that has available funds. When this list has more than one
     * entry, a pool-selection widget is shown in the Exchange Rate step. When it has exactly
     * one entry the selection is applied automatically (no UI shown). Empty when no withdrawals
     * are available at all.
     */
    val availableWithdrawalPools: ImmutableList<WithdrawalPoolOptionUiModel> = persistentListOf(),
    /**
     * The pool the user has explicitly selected (or auto-selected when only one pool exists).
     * Passed to [PreviewCashExchangeRateUseCase] for the rate preview and to [AddExpenseUseCase]
     * at submission time to direct the FIFO deduction. Null when no pool has been resolved yet
     * (e.g., no withdrawals available or payment method is not CASH).
     */
    val selectedWithdrawalPool: WithdrawalPoolOptionUiModel? = null,
    /**
     * Non-null when a personal (USER/SUBUNIT) cash pool is selected and the current split includes
     * members outside that pool's natural scope. Contains the warning message to display.
     * Null when no warning should be shown.
     */
    val personalCashSplitWarning: UiText? = null,
    /**
     * True when the exchange rate was served from an expired local cache
     * (the remote API was unreachable). Drives a warning banner in the
     * exchange rate section.
     */
    val isExchangeRateStale: Boolean = false,
    val showDueDateSection: Boolean = false,

    // Due date
    val dueDateMillis: Long? = null,
    val formattedDueDate: String = "",

    // Receipt image
    val receiptUri: String? = null,

    // Pre-formatted labels for the exchange rate section
    val exchangeRateLabel: String = "",
    val groupAmountLabel: String = "",

    // Data Lists
    val availableCurrencies: ImmutableList<CurrencyUiModel> = persistentListOf(),
    val paymentMethods: ImmutableList<PaymentMethodUiModel> = persistentListOf(),
    val fundingSources: ImmutableList<FundingSourceUiModel> = persistentListOf(),
    val availableCategories: ImmutableList<CategoryUiModel> = persistentListOf(),
    val availablePaymentStatuses: ImmutableList<PaymentStatusUiModel> = persistentListOf(),

    // Split Configuration
    val availableSplitTypes: ImmutableList<SplitTypeUiModel> = persistentListOf(),
    val selectedSplitType: SplitTypeUiModel? = null,
    val splits: ImmutableList<SplitUiModel> = persistentListOf(),
    val splitError: UiText? = null,
    val memberIds: ImmutableList<String> = persistentListOf(),

    // Add-On Configuration
    val addOns: ImmutableList<AddOnUiModel> = persistentListOf(),
    val isAddOnsSectionExpanded: Boolean = false,
    val addOnError: UiText? = null,
    /** Formatted effective total (base + ON_TOP add-ons − discounts) for display. */
    val effectiveTotal: String = "",
    /** Formatted base cost when INCLUDED add-ons are present. Empty otherwise. */
    val includedBaseCost: String = "",

    // Subunit split mode
    /** True when the group has subunits available (controls toggle visibility). */
    val hasSubunits: Boolean = false,
    /** True when "Split by subunit" toggle is active. */
    val isSubunitMode: Boolean = false,
    /** Entity-level splits (solo users + subunit headers) for subunit mode. */
    val entitySplits: ImmutableList<SplitUiModel> = persistentListOf(),

    // Contribution scope (out-of-pocket)
    /** Selected contribution scope for the paired contribution (USER / SUBUNIT / GROUP). */
    val contributionScope: PayerType = PayerType.USER,
    /** Selected subunit ID when contribution scope is SUBUNIT. */
    val selectedContributionSubunitId: String? = null,
    /** Subunits the current user belongs to — drives the scope step's subunit picker. */
    val contributionSubunitOptions: ImmutableList<SubunitOptionUiModel> = persistentListOf(),

    // Errors
    val error: UiText? = null,
    val isTitleValid: Boolean = true,
    val isAmountValid: Boolean = true,
    val isDueDateValid: Boolean = true,

    // ── Wizard ──────────────────────────────────────────────────────────
    val currentStep: AddExpenseStep = AddExpenseStep.TITLE,
    /**
     * When non-null, the user jumped from this step to REVIEW via "Skip to Review".
     * Pressing Back on REVIEW returns to this step instead of the previous sequential one.
     */
    val jumpedFromStep: AddExpenseStep? = null
) {
    /**
     * Returns true when the screen is ready for user interaction.
     * The form should only be shown when config is loaded and not failed.
     */
    val isReady: Boolean
        get() = isConfigLoaded && !configLoadFailed && !isLoading

    /**
     * Returns true when the form inputs are valid and ready for submission.
     */
    val isFormValid: Boolean
        get() = isTitleValid &&
            isAmountValid &&
            isDueDateValid &&
            addOns.all { it.isAmountValid } &&
            expenseTitle.isNotBlank() &&
            sourceAmount.isNotBlank()

    // ── Wizard computed properties ──────────────────────────────────────

    /** True when the contribution scope step should be shown (funding source = "My Money"). */
    val showContributionScopeStep: Boolean
        get() = selectedFundingSource?.id == PayerType.USER.name

    /** Ordered list of steps that are currently applicable. */
    val applicableSteps: List<AddExpenseStep>
        get() = AddExpenseStep.applicableSteps(
            showContributionScopeStep = showContributionScopeStep,
            showExchangeRateSection = showExchangeRateSection,
            hasSplit = memberIds.size > 1
        )

    /** Zero-based index of the current step within [applicableSteps]. */
    val currentStepIndex: Int
        get() = applicableSteps.indexOf(currentStep).coerceAtLeast(0)

    /** Whether the wizard can navigate to the next step. */
    val canGoNext: Boolean
        get() = currentStepIndex < applicableSteps.lastIndex

    /** Whether the current step is the final review step. */
    val isOnReviewStep: Boolean
        get() = currentStep == AddExpenseStep.REVIEW

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
     * back to the group currency removes the EXCHANGE_RATE step, or reducing members
     * removes the SPLIT step). Without clamping, [currentStep] could point to a step
     * that is no longer in [applicableSteps].
     */
    fun withStepClamped(): AddExpenseUiState {
        val steps = applicableSteps
        if (currentStep in steps) return this
        val clampedStep = steps.lastOrNull { it.ordinal < currentStep.ordinal } ?: steps.first()
        return copy(currentStep = clampedStep)
    }

    /** Whether the current step's fields pass validation (gates the "Next" button). */
    val isCurrentStepValid: Boolean
        get() = when (currentStep) {
            AddExpenseStep.TITLE ->
                expenseTitle.isNotBlank() && isTitleValid

            AddExpenseStep.PAYMENT_METHOD -> true // selection is provided by loaded config

            AddExpenseStep.FUNDING_SOURCE -> true // selection is provided by loaded config

            AddExpenseStep.CONTRIBUTION_SCOPE ->
                contributionScope != PayerType.SUBUNIT || selectedContributionSubunitId != null

            AddExpenseStep.AMOUNT ->
                sourceAmount.isNotBlank() &&
                    isAmountValid &&
                    !(isInsufficientCash && !showExchangeRateSection) &&
                    // For same-currency CASH with multiple pools, a pool must be resolved (either
                    // auto-selected by WithdrawalPoolSelectionDelegate or explicitly by the user)
                    // before the user can advance. The pool selector is shown in AmountStep because
                    // there is no ExchangeRateStep for same-currency expenses.
                    (showExchangeRateSection || availableWithdrawalPools.size <= 1 || selectedWithdrawalPool != null)

            AddExpenseStep.EXCHANGE_RATE ->
                displayExchangeRate.isNotBlank() && calculatedGroupAmount.isNotBlank()

            AddExpenseStep.SPLIT -> splitError == null

            AddExpenseStep.CATEGORY -> true // optional

            AddExpenseStep.VENDOR_NOTES -> true // optional

            AddExpenseStep.PAYMENT_STATUS ->
                !showDueDateSection || isDueDateValid

            AddExpenseStep.RECEIPT -> true // optional

            AddExpenseStep.ADD_ONS -> addOns.all { it.isAmountValid } && addOnError == null

            AddExpenseStep.REVIEW -> isFormValid
        }
}
