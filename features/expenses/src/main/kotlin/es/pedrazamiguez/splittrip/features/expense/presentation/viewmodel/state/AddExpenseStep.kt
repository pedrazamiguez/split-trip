package es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state

import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.WizardStep

/**
 * Defines the wizard steps for the Add Expense flow.
 *
 * The step sequence is:
 *   1. title (always)
 *   2. payment method (always — determines exchange-rate lock behaviour)
 *   3. amount + currency (always)
 *   4. exchange rate (conditional: foreign currency selected; optional — defaults to last-used/fetched rate)
 *   5. sub-expenses / payment tranches (always, optional — may be skipped)
 *   6. split among members (conditional: group has >1 member; optional — defaults to equal split)
 *   7. category (always, optional — may be skipped)
 *   8. funding source (always, optional — defaults to "Group Pocket")
 *   9. contribution scope (conditional: funding source = "My Money"; mandatory when shown)
 *  10. vendor + notes (always, optional — may be skipped)
 *  11. payment status / scheduling (conditional: only when sub-expenses disabled; optional — defaults to "Paid")
 *  12. receipt (always, optional — may be skipped)
 *  13. add-ons: fees, tips, discounts, surcharges (always, optional — may be skipped)
 *  14. review: read-only summary (always)
 *
 * Conditional steps (CONTRIBUTION_SCOPE, EXCHANGE_RATE, SPLIT, PAYMENT_STATUS) are dynamically
 * included/excluded by [applicableSteps] based on the current form state.
 *
 * The guiding philosophy is: **"As easy or as complicated to log an expense as the user needs."**
 * The minimum viable path is: TITLE → PAYMENT_METHOD → AMOUNT → [Skip to Review] → REVIEW.
 * All optional steps carry sensible pre-filled defaults so they can be skipped without leaving
 * the expense incomplete.
 *
 * @property isOptional When `true` the step can be skipped via the "Skip to Review"
 *                      link in [WizardStepIndicator]. Optional steps show a dashed
 *                      border in the indicator until completed.
 * @property isReview When `true` this is the final read-only review/confirmation step.
 */
enum class AddExpenseStep(
    override val isOptional: Boolean = false,
    override val isReview: Boolean = false
) : WizardStep {
    /** Expense title — always shown. */
    TITLE,

    /** Payment method selection — always shown (affects exchange-rate lock). */
    PAYMENT_METHOD,

    /** Amount + Currency — always shown. */
    AMOUNT,

    /**
     * Exchange rate + calculated group amount — only when foreign currency selected.
     * Optional: defaults to last-used or API-fetched rate when skipped.
     */
    EXCHANGE_RATE(isOptional = true),

    /** Sub-expenses / payment tranches — optional, may be skipped. */
    SUB_EXPENSES(isOptional = true),

    /**
     * Split type + per-member allocation + sub-unit mode — only when >1 member.
     * Optional: defaults to equal split among all members when skipped.
     */
    SPLIT(isOptional = true),

    /** Category selection — optional, may be skipped. */
    CATEGORY(isOptional = true),

    /**
     * Funding source — always shown ("Group Pocket" or "My Money").
     * Optional: defaults to "Group Pocket" when skipped.
     */
    FUNDING_SOURCE(isOptional = true),

    /**
     * Contribution scope — only when funding source is "My Money" (USER).
     * Mandatory when shown: the user must choose a scope explicitly.
     */
    CONTRIBUTION_SCOPE,

    /** Vendor and optional notes — optional, may be skipped. */
    VENDOR_NOTES(isOptional = true),

    /**
     * Payment status + conditional due date — only shown when sub-expenses disabled.
     * Optional: defaults to "Paid" (not scheduled) when skipped.
     */
    PAYMENT_STATUS(isOptional = true),

    /** Receipt image attachment — optional, may be skipped. */
    RECEIPT(isOptional = true),

    /** Fees, tips, discounts, surcharges — optional, may be skipped. */
    ADD_ONS(isOptional = true),

    /** Read-only summary of all entered data — always shown (final confirmation). */
    REVIEW(isReview = true);

    companion object {
        /**
         * Returns the ordered list of steps applicable to the current form state.
         *
         * @param showContributionScopeStep `true` when funding source is "My Money" (USER).
         * @param showExchangeRateSection `true` when a foreign currency is selected.
         * @param hasSplit `true` when the group has more than one member.
         * @param isSubExpensesEnabled `true` when sub-expenses (payment tranches) mode is active.
         */
        fun applicableSteps(
            showContributionScopeStep: Boolean,
            showExchangeRateSection: Boolean,
            hasSplit: Boolean,
            isSubExpensesEnabled: Boolean = false
        ): List<AddExpenseStep> = buildList {
            add(TITLE)
            add(PAYMENT_METHOD)
            add(AMOUNT)
            if (showExchangeRateSection) add(EXCHANGE_RATE)
            add(SUB_EXPENSES)
            if (hasSplit) add(SPLIT)
            add(CATEGORY)
            add(FUNDING_SOURCE)
            if (showContributionScopeStep) add(CONTRIBUTION_SCOPE)
            add(VENDOR_NOTES)
            if (!isSubExpensesEnabled) add(PAYMENT_STATUS)
            add(RECEIPT)
            add(ADD_ONS)
            add(REVIEW)
        }
    }
}
