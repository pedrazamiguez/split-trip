package es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state

import androidx.annotation.StringRes
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.WizardStep
import es.pedrazamiguez.splittrip.features.expense.R
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * Defines the wizard steps for the Add Expense flow.
 *
 * The step sequence is:
 *   1. title (always)
 *   2. payment method (always — determines exchange-rate lock behaviour)
 *   3. amount + currency (always)
 *   4. exchange rate (conditional: foreign currency selected; optional — defaults to last-used/fetched rate)
 *   5. split among members (conditional: group has >1 member; optional — defaults to equal split)
 *   6. category (always, optional — may be skipped)
 *   7. funding source (always, optional — defaults based on group pocket activity)
 *   8. contribution scope (conditional: funding source = "Your Money"; optional — defaults to "For you")
 *   9. vendor + notes (always, optional — may be skipped)
 *  10. payment status / scheduling (always, optional — defaults to "Paid")
 *  11. receipt (always, optional — may be skipped)
 *  12. add-ons: fees, tips, discounts, surcharges (always, optional — may be skipped)
 *  13. review: read-only summary (always)
 *
 * Conditional steps (CONTRIBUTION_SCOPE, EXCHANGE_RATE, SPLIT) are dynamically
 * included/excluded by [applicableSteps] based on the current form state.
 *
 * The guiding philosophy is: **"As easy or as complicated to log an expense as the user needs."**
 * The minimum viable path is: TITLE → PAYMENT_METHOD → AMOUNT → [Skip to Review] → REVIEW.
 * All optional steps carry sensible pre-filled defaults so they can be skipped without leaving
 * the expense incomplete.
 *
 * @property infoTitleRes String resource ID for the contextual step info bottom sheet title.
 * @property infoDescriptionRes String resource ID for the contextual step info bottom sheet body.
 * @property infoItems Optional structured option items displayed below the intro description.
 * @property isOptional When `true` the step can be skipped via the "Skip to Review"
 *                      link in [WizardStepIndicator]. Optional steps show a dashed
 *                      border in the indicator until completed.
 * @property isReview When `true` this is the final read-only review/confirmation step.
 */
enum class AddExpenseStep(
    @StringRes val infoTitleRes: Int,
    @StringRes val infoDescriptionRes: Int,
    val infoItems: ImmutableList<ExpenseStepInfoItem> = persistentListOf(),
    override val isOptional: Boolean = false,
    override val isReview: Boolean = false
) : WizardStep {
    /** Expense title — always shown. */
    TITLE(
        infoTitleRes = R.string.expense_step_info_title_title,
        infoDescriptionRes = R.string.expense_step_info_title_description
    ),

    /** Payment method selection — always shown (affects exchange-rate lock). */
    PAYMENT_METHOD(
        infoTitleRes = R.string.expense_step_info_payment_method_title,
        infoDescriptionRes = R.string.expense_step_info_payment_method_description
    ),

    /** Amount + Currency — always shown. */
    AMOUNT(
        infoTitleRes = R.string.expense_step_info_amount_title,
        infoDescriptionRes = R.string.expense_step_info_amount_description
    ),

    /**
     * Exchange rate + calculated group amount — only when foreign currency selected.
     * Optional: defaults to last-used or API-fetched rate when skipped.
     */
    EXCHANGE_RATE(
        infoTitleRes = R.string.expense_step_info_exchange_rate_title,
        infoDescriptionRes = R.string.expense_step_info_exchange_rate_description,
        isOptional = true
    ),

    /**
     * Split type + per-member allocation + sub-unit mode — only when >1 member.
     * Optional: defaults to equal split among all members when skipped.
     */
    SPLIT(
        infoTitleRes = R.string.expense_step_info_split_title,
        infoDescriptionRes = R.string.expense_step_info_split_description,
        infoItems = persistentListOf(
            ExpenseStepInfoItem(
                titleRes = R.string.expense_step_info_split_item_equal_title,
                descriptionRes = R.string.expense_step_info_split_item_equal_description
            ),
            ExpenseStepInfoItem(
                titleRes = R.string.expense_step_info_split_item_exact_title,
                descriptionRes = R.string.expense_step_info_split_item_exact_description
            ),
            ExpenseStepInfoItem(
                titleRes = R.string.expense_step_info_split_item_percent_title,
                descriptionRes = R.string.expense_step_info_split_item_percent_description
            )
        ),
        isOptional = true
    ),

    /** Category selection — optional, may be skipped. */
    CATEGORY(
        infoTitleRes = R.string.expense_step_info_category_title,
        infoDescriptionRes = R.string.expense_step_info_category_description,
        isOptional = true
    ),

    /**
     * Funding source — always shown ("Group Pocket" or "Your Money").
     * Optional: defaults to "Your Money" when no manual pocket activity exists, or "Group Pocket" otherwise.
     */
    FUNDING_SOURCE(
        infoTitleRes = R.string.expense_step_info_funding_source_title,
        infoDescriptionRes = R.string.expense_step_info_funding_source_description,
        infoItems = persistentListOf(
            ExpenseStepInfoItem(
                titleRes = R.string.expense_step_info_funding_source_item_your_money_title,
                descriptionRes = R.string.expense_step_info_funding_source_item_your_money_description
            ),
            ExpenseStepInfoItem(
                titleRes = R.string.expense_step_info_funding_source_item_group_pocket_title,
                descriptionRes = R.string.expense_step_info_funding_source_item_group_pocket_description
            )
        ),
        isOptional = true
    ),

    /**
     * Contribution scope — only when funding source is "Your Money" (USER).
     * Optional: defaults to USER ("For you") when skipped.
     */
    CONTRIBUTION_SCOPE(
        infoTitleRes = R.string.expense_step_info_contribution_scope_title,
        infoDescriptionRes = R.string.expense_step_info_contribution_scope_description,
        infoItems = persistentListOf(
            ExpenseStepInfoItem(
                titleRes = R.string.expense_step_info_contribution_scope_item_for_you_title,
                descriptionRes = R.string.expense_step_info_contribution_scope_item_for_you_description
            ),
            ExpenseStepInfoItem(
                titleRes = R.string.expense_step_info_contribution_scope_item_for_subunit_title,
                descriptionRes = R.string.expense_step_info_contribution_scope_item_for_subunit_description
            ),
            ExpenseStepInfoItem(
                titleRes = R.string.expense_step_info_contribution_scope_item_for_group_title,
                descriptionRes = R.string.expense_step_info_contribution_scope_item_for_group_description
            )
        ),
        isOptional = true
    ),

    /** Vendor and optional notes — optional, may be skipped. */
    VENDOR_NOTES(
        infoTitleRes = R.string.expense_step_info_vendor_notes_title,
        infoDescriptionRes = R.string.expense_step_info_vendor_notes_description,
        isOptional = true
    ),

    /**
     * Payment status + conditional due date — always shown.
     * Optional: defaults to "Paid" (not scheduled) when skipped.
     */
    PAYMENT_STATUS(
        infoTitleRes = R.string.expense_step_info_payment_status_title,
        infoDescriptionRes = R.string.expense_step_info_payment_status_description,
        infoItems = persistentListOf(
            ExpenseStepInfoItem(
                titleRes = R.string.expense_step_info_payment_status_item_paid_title,
                descriptionRes = R.string.expense_step_info_payment_status_item_paid_description
            ),
            ExpenseStepInfoItem(
                titleRes = R.string.expense_step_info_payment_status_item_scheduled_title,
                descriptionRes = R.string.expense_step_info_payment_status_item_scheduled_description
            ),
            ExpenseStepInfoItem(
                titleRes = R.string.expense_step_info_payment_status_item_refundable_title,
                descriptionRes = R.string.expense_step_info_payment_status_item_refundable_description
            ),
            ExpenseStepInfoItem(
                titleRes = R.string.expense_step_info_payment_status_item_cancelled_title,
                descriptionRes = R.string.expense_step_info_payment_status_item_cancelled_description
            )
        ),
        isOptional = true
    ),

    /** Receipt image attachment — optional, may be skipped. */
    RECEIPT(
        infoTitleRes = R.string.expense_step_info_receipt_title,
        infoDescriptionRes = R.string.expense_step_info_receipt_description,
        isOptional = true
    ),

    /** Fees, tips, discounts, surcharges — optional, may be skipped. */
    ADD_ONS(
        infoTitleRes = R.string.expense_step_info_add_ons_title,
        infoDescriptionRes = R.string.expense_step_info_add_ons_description,
        infoItems = persistentListOf(
            ExpenseStepInfoItem(
                titleRes = R.string.expense_step_info_add_ons_item_on_top_title,
                descriptionRes = R.string.expense_step_info_add_ons_item_on_top_description
            ),
            ExpenseStepInfoItem(
                titleRes = R.string.expense_step_info_add_ons_item_included_title,
                descriptionRes = R.string.expense_step_info_add_ons_item_included_description
            )
        ),
        isOptional = true
    ),

    /** Read-only summary of all entered data — always shown (final confirmation). */
    REVIEW(
        infoTitleRes = R.string.expense_step_info_review_title,
        infoDescriptionRes = R.string.expense_step_info_review_description,
        isReview = true
    );

    companion object {
        /**
         * Returns the ordered list of steps applicable to the current form state.
         *
         * @param showContributionScopeStep `true` when funding source is "Your Money" (USER).
         * @param showExchangeRateSection `true` when a foreign currency is selected.
         * @param hasSplit `true` when the group has more than one member.
         */
        fun applicableSteps(
            showContributionScopeStep: Boolean,
            showExchangeRateSection: Boolean,
            hasSplit: Boolean
        ): List<AddExpenseStep> = buildList {
            add(TITLE)
            add(PAYMENT_METHOD)
            add(AMOUNT)
            if (showExchangeRateSection) add(EXCHANGE_RATE)
            if (hasSplit) add(SPLIT)
            add(CATEGORY)
            add(FUNDING_SOURCE)
            if (showContributionScopeStep) add(CONTRIBUTION_SCOPE)
            add(VENDOR_NOTES)
            add(PAYMENT_STATUS)
            add(RECEIPT)
            add(ADD_ONS)
            add(REVIEW)
        }
    }
}
