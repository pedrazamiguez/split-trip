package es.pedrazamiguez.splittrip.features.withdrawal.presentation.viewmodel.state

import androidx.annotation.StringRes
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.WizardStep
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.WizardStepInfoItem
import es.pedrazamiguez.splittrip.features.withdrawal.R
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * Defines the wizard steps for the cash withdrawal flow.
 *
 * The step sequence is intentionally symmetric:
 *   - withdrawal amount → withdrawal conversion (if foreign)
 *   - scope → details (with fee toggle)
 *   - fee amount → fee conversion (if fee currency is foreign)
 *   - review
 *
 * Conditional steps (EXCHANGE_RATE, ATM_FEE, FEE_EXCHANGE_RATE) are dynamically
 * included/excluded by [applicableSteps] based on the current form state.
 *
 * @property infoTitleRes String resource ID for the contextual step info bottom sheet title.
 * @property infoDescriptionRes String resource ID for the contextual step info bottom sheet body.
 * @property infoItems Optional structured option items displayed below the intro description.
 * @property isOptional When `true` the step can be skipped via the "Skip to Review"
 *                      link in [WizardStepIndicator]. Optional steps show a dashed
 *                      border in the indicator until completed.
 * @property isReview When `true` this is the final read-only review/confirmation step.
 */
enum class CashWithdrawalStep(
    @StringRes val infoTitleRes: Int,
    @StringRes val infoDescriptionRes: Int,
    val infoItems: ImmutableList<WizardStepInfoItem> = persistentListOf(),
    override val isOptional: Boolean = false,
    override val isReview: Boolean = false
) : WizardStep {
    /** Amount input + currency selector — always shown. */
    AMOUNT(
        infoTitleRes = R.string.withdrawal_step_info_amount_title,
        infoDescriptionRes = R.string.withdrawal_step_info_amount_description
    ),

    /** Exchange rate + deducted amount in group currency — only when foreign currency selected. */
    EXCHANGE_RATE(
        infoTitleRes = R.string.withdrawal_step_info_exchange_rate_title,
        infoDescriptionRes = R.string.withdrawal_step_info_exchange_rate_description
    ),

    /** Who is withdrawing: group / subunit / personal — always shown. */
    SCOPE(
        infoTitleRes = R.string.withdrawal_step_info_scope_title,
        infoDescriptionRes = R.string.withdrawal_step_info_scope_description,
        infoItems = persistentListOf(
            WizardStepInfoItem(
                titleRes = R.string.withdrawal_step_info_scope_item_for_group_title,
                descriptionRes = R.string.withdrawal_step_info_scope_item_for_group_description
            ),
            WizardStepInfoItem(
                titleRes = R.string.withdrawal_step_info_scope_item_for_subunit_title,
                descriptionRes = R.string.withdrawal_step_info_scope_item_for_subunit_description
            ),
            WizardStepInfoItem(
                titleRes = R.string.withdrawal_step_info_scope_item_for_you_title,
                descriptionRes = R.string.withdrawal_step_info_scope_item_for_you_description
            )
        )
    ),

    /** Title, notes, and ATM fee opt-in toggle — always shown, optional. */
    DETAILS(
        infoTitleRes = R.string.withdrawal_step_info_details_title,
        infoDescriptionRes = R.string.withdrawal_step_info_details_description,
        isOptional = true
    ),

    /** ATM fee amount + fee currency — only when fee is toggled on. */
    ATM_FEE(
        infoTitleRes = R.string.withdrawal_step_info_atm_fee_title,
        infoDescriptionRes = R.string.withdrawal_step_info_atm_fee_description
    ),

    /** ATM fee exchange rate + converted amount — only when fee currency ≠ group currency. */
    FEE_EXCHANGE_RATE(
        infoTitleRes = R.string.withdrawal_step_info_fee_exchange_rate_title,
        infoDescriptionRes = R.string.withdrawal_step_info_fee_exchange_rate_description
    ),

    /** Read-only summary of all entered data — always shown (final confirmation). */
    REVIEW(
        infoTitleRes = R.string.withdrawal_step_info_review_title,
        infoDescriptionRes = R.string.withdrawal_step_info_review_description,
        isReview = true
    );

    companion object {
        /**
         * Returns the ordered list of steps applicable to the current form state.
         *
         * @param showExchangeRateSection `true` when a foreign withdrawal currency is selected.
         * @param hasFee                  `true` when the ATM fee toggle is on.
         * @param showFeeExchangeRateSection `true` when fee currency ≠ group currency.
         */
        fun applicableSteps(
            showExchangeRateSection: Boolean,
            hasFee: Boolean,
            showFeeExchangeRateSection: Boolean
        ): List<CashWithdrawalStep> = buildList {
            add(AMOUNT)
            if (showExchangeRateSection) add(EXCHANGE_RATE)
            add(SCOPE)
            add(DETAILS)
            if (hasFee) {
                add(ATM_FEE)
                if (showFeeExchangeRateSection) add(FEE_EXCHANGE_RATE)
            }
            add(REVIEW)
        }
    }
}
