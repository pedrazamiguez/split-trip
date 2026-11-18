package es.pedrazamiguez.splittrip.features.withdrawal.presentation.component.step

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.SectionCard
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.rememberLocale
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.WizardReviewRow
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.WizardStepLayout
import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.formatAmountWithCurrency
import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.features.withdrawal.R
import es.pedrazamiguez.splittrip.features.withdrawal.presentation.viewmodel.state.AddCashWithdrawalUiState

/**
 * Step 5: Read-only summary of all entered data (final confirmation).
 * Always shown as the last wizard step.
 */
@Suppress("LongMethod", "CyclomaticComplexMethod", "CognitiveComplexMethod")
@Composable
fun ReviewStep(
    uiState: AddCashWithdrawalUiState,
    modifier: Modifier = Modifier
) {
    val none = stringResource(R.string.withdrawal_review_none)
    val locale = rememberLocale()

    WizardStepLayout(modifier = modifier) {
        SectionCard(title = stringResource(R.string.withdrawal_review_title)) {
            WizardReviewRow(
                label = stringResource(R.string.withdrawal_review_amount),
                value = uiState.selectedCurrency?.code?.let { code ->
                    formatAmountWithCurrency(uiState.withdrawalAmount, code, locale)
                }?.ifBlank { none } ?: uiState.withdrawalAmount.ifBlank { none }
            )
            WizardReviewRow(
                label = stringResource(R.string.withdrawal_review_currency),
                value = uiState.selectedCurrency?.displayText ?: none
            )

            if (uiState.showExchangeRateSection) {
                WizardReviewRow(
                    label = stringResource(R.string.withdrawal_review_exchange_rate),
                    value = uiState.displayExchangeRate
                )
                WizardReviewRow(
                    label = stringResource(R.string.withdrawal_review_deducted),
                    value = uiState.groupCurrency?.code?.let { code ->
                        formatAmountWithCurrency(uiState.deductedAmount, code, locale)
                    }?.ifBlank { none } ?: uiState.deductedAmount.ifBlank { none }
                )
            }

            if (uiState.hasFee && uiState.feeAmount.isNotBlank()) {
                WizardReviewRow(
                    label = stringResource(R.string.withdrawal_review_atm_fee),
                    value = (uiState.feeCurrency ?: uiState.groupCurrency)?.code?.let { code ->
                        formatAmountWithCurrency(uiState.feeAmount, code, locale)
                    } ?: uiState.feeAmount
                )
                if (uiState.showFeeExchangeRateSection && uiState.feeConvertedAmount.isNotBlank()) {
                    WizardReviewRow(
                        label = stringResource(R.string.withdrawal_review_fee_converted),
                        value = uiState.groupCurrency?.code?.let { code ->
                            formatAmountWithCurrency(uiState.feeConvertedAmount, code, locale)
                        } ?: uiState.feeConvertedAmount
                    )
                }
            }

            WizardReviewRow(
                label = stringResource(R.string.withdrawal_review_member),
                value = uiState.selectedMemberDisplayName.ifBlank { none }
            )
            WizardReviewRow(
                label = stringResource(R.string.withdrawal_review_scope),
                value = when (uiState.withdrawalScope) {
                    PayerType.GROUP -> stringResource(R.string.withdrawal_scope_group)
                    PayerType.USER -> stringResource(R.string.withdrawal_scope_personal)
                    PayerType.SUBUNIT ->
                        uiState.subunitOptions
                            .find { it.id == uiState.selectedSubunitId }?.name ?: none
                }
            )
            if (uiState.title.isNotBlank()) {
                WizardReviewRow(
                    label = stringResource(R.string.withdrawal_review_withdrawal_title),
                    value = uiState.title
                )
            }
            if (uiState.notes.isNotBlank()) {
                WizardReviewRow(
                    label = stringResource(R.string.withdrawal_review_notes),
                    value = uiState.notes
                )
            }
        }
    }
}
