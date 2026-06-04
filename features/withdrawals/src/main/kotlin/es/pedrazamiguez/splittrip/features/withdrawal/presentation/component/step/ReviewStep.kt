package es.pedrazamiguez.splittrip.features.withdrawal.presentation.component.step

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.SectionCard
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.rememberLocale
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.BodyText
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
            // Amount Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                BodyText(
                    text = stringResource(R.string.withdrawal_review_amount),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = uiState.selectedCurrency?.code?.let { code ->
                        formatAmountWithCurrency(uiState.withdrawalAmount, code, locale)
                    }?.ifBlank { none } ?: uiState.withdrawalAmount.ifBlank { none },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                BodyText(
                    text = stringResource(R.string.withdrawal_review_currency),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = uiState.selectedCurrency?.displayText ?: none,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (uiState.showExchangeRateSection) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    BodyText(
                        text = stringResource(R.string.withdrawal_review_exchange_rate),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = uiState.displayExchangeRate,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    BodyText(
                        text = stringResource(R.string.withdrawal_review_deducted),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = uiState.groupCurrency?.code?.let { code ->
                            formatAmountWithCurrency(uiState.deductedAmount, code, locale)
                        }?.ifBlank { none } ?: uiState.deductedAmount.ifBlank { none },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Fee Section
            if (uiState.hasFee && uiState.feeAmount.isNotBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    BodyText(
                        text = stringResource(R.string.withdrawal_review_atm_fee),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = (uiState.feeCurrency ?: uiState.groupCurrency)?.code?.let { code ->
                            formatAmountWithCurrency(uiState.feeAmount, code, locale)
                        } ?: uiState.feeAmount,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                if (uiState.showFeeExchangeRateSection && uiState.feeConvertedAmount.isNotBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        BodyText(
                            text = stringResource(R.string.withdrawal_review_fee_converted),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = uiState.groupCurrency?.code?.let { code ->
                                formatAmountWithCurrency(uiState.feeConvertedAmount, code, locale)
                            } ?: uiState.feeConvertedAmount,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Details Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                BodyText(
                    text = stringResource(R.string.withdrawal_review_member),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = uiState.selectedMemberDisplayName.ifBlank { none },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                BodyText(
                    text = stringResource(R.string.withdrawal_review_scope),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = when (uiState.withdrawalScope) {
                        PayerType.GROUP -> stringResource(R.string.withdrawal_scope_group)
                        PayerType.USER -> stringResource(R.string.withdrawal_scope_personal)
                        PayerType.SUBUNIT ->
                            uiState.subunitOptions
                                .find { it.id == uiState.selectedSubunitId }?.name ?: none
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            if (uiState.title.isNotBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    BodyText(
                        text = stringResource(R.string.withdrawal_review_withdrawal_title),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = uiState.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            if (uiState.notes.isNotBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    BodyText(
                        text = stringResource(R.string.withdrawal_review_notes),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = uiState.notes,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
