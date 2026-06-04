package es.pedrazamiguez.splittrip.features.expense.presentation.component.step.expense

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.SectionCard
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.rememberLocale
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.BodyText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.WizardStepLayout
import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.formatAmountWithCurrency
import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.AddExpenseUiState

private const val LABEL_WEIGHT = 0.4f
private const val VALUE_WEIGHT = 0.6f

/**
 * Step 11 (final): Read-only summary of all entered data.
 * Shows amounts, exchange rate (if foreign), detail fields, split, and add-ons.
 */
@Suppress("LongMethod", "CyclomaticComplexMethod", "CognitiveComplexMethod")
@Composable
fun ReviewStep(
    uiState: AddExpenseUiState,
    modifier: Modifier = Modifier
) {
    val none = stringResource(R.string.expense_review_none)
    val locale = rememberLocale()

    WizardStepLayout(modifier = modifier) {
        SectionCard(
            title = stringResource(R.string.expense_review_title)
        ) {
            // ── Amount Section ───────────────────────────────────────────────────
            val titleVal = uiState.expenseTitle.ifBlank { none }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                BodyText(
                    text = stringResource(R.string.expense_review_title_label),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(LABEL_WEIGHT)
                )
                Text(
                    text = titleVal,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(VALUE_WEIGHT),
                    textAlign = TextAlign.End
                )
            }

            val amountVal = uiState.selectedCurrency?.code?.let { code ->
                formatAmountWithCurrency(uiState.sourceAmount, code, locale)
            }?.ifBlank { none } ?: uiState.sourceAmount.ifBlank { none }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                BodyText(
                    text = stringResource(R.string.expense_review_amount),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(LABEL_WEIGHT)
                )
                Text(
                    text = amountVal,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(VALUE_WEIGHT),
                    textAlign = TextAlign.End
                )
            }

            val currencyVal = uiState.selectedCurrency?.displayText ?: none
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                BodyText(
                    text = stringResource(R.string.expense_review_currency),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(LABEL_WEIGHT)
                )
                Text(
                    text = currencyVal,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(VALUE_WEIGHT),
                    textAlign = TextAlign.End
                )
            }

            if (uiState.showExchangeRateSection) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    BodyText(
                        text = stringResource(R.string.expense_review_exchange_rate),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(LABEL_WEIGHT)
                    )
                    Text(
                        text = uiState.displayExchangeRate,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(VALUE_WEIGHT),
                        textAlign = TextAlign.End
                    )
                }

                val groupAmountVal = uiState.groupCurrency?.code?.let { code ->
                    formatAmountWithCurrency(uiState.calculatedGroupAmount, code, locale)
                }?.ifBlank { none } ?: uiState.calculatedGroupAmount.ifBlank { none }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    BodyText(
                        text = stringResource(R.string.expense_review_group_amount),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(LABEL_WEIGHT)
                    )
                    Text(
                        text = groupAmountVal,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(VALUE_WEIGHT),
                        textAlign = TextAlign.End
                    )
                }
            }

            // ── Details Section ──────────────────────────────────────────────────
            val hasAnyDetail = uiState.selectedPaymentMethod != null ||
                uiState.selectedFundingSource != null ||
                uiState.selectedCategory != null ||
                uiState.vendor.isNotBlank() ||
                uiState.notes.isNotBlank() ||
                uiState.selectedPaymentStatus != null ||
                uiState.formattedDueDate.isNotBlank() ||
                uiState.receiptUri != null

            if (hasAnyDetail) {
                uiState.selectedPaymentMethod?.let {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        BodyText(
                            text = stringResource(R.string.expense_review_payment_method),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(LABEL_WEIGHT)
                        )
                        Text(
                            text = it.displayText,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(VALUE_WEIGHT),
                            textAlign = TextAlign.End
                        )
                    }
                }
                uiState.selectedFundingSource?.let {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        BodyText(
                            text = stringResource(R.string.expense_review_funding_source),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(LABEL_WEIGHT)
                        )
                        Text(
                            text = it.displayText,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(VALUE_WEIGHT),
                            textAlign = TextAlign.End
                        )
                    }
                }
                if (uiState.showContributionScopeStep) {
                    val scopeVal = when (uiState.contributionScope) {
                        PayerType.GROUP -> stringResource(R.string.expense_review_scope_group)
                        PayerType.SUBUNIT ->
                            uiState.contributionSubunitOptions
                                .find { it.id == uiState.selectedContributionSubunitId }
                                ?.name ?: none
                        else -> stringResource(R.string.expense_review_scope_personal)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        BodyText(
                            text = stringResource(R.string.expense_review_contribution_scope),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(LABEL_WEIGHT)
                        )
                        Text(
                            text = scopeVal,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(VALUE_WEIGHT),
                            textAlign = TextAlign.End
                        )
                    }
                }
                uiState.selectedCategory?.let {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        BodyText(
                            text = stringResource(R.string.expense_review_category),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(LABEL_WEIGHT)
                        )
                        Text(
                            text = it.displayText,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(VALUE_WEIGHT),
                            textAlign = TextAlign.End
                        )
                    }
                }
                if (uiState.vendor.isNotBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        BodyText(
                            text = stringResource(R.string.expense_review_vendor),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(LABEL_WEIGHT)
                        )
                        Text(
                            text = uiState.vendor,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(VALUE_WEIGHT),
                            textAlign = TextAlign.End
                        )
                    }
                }
                if (uiState.notes.isNotBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        BodyText(
                            text = stringResource(R.string.expense_review_notes),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(LABEL_WEIGHT)
                        )
                        Text(
                            text = uiState.notes,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(VALUE_WEIGHT),
                            textAlign = TextAlign.End
                        )
                    }
                }
                uiState.selectedPaymentStatus?.let {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        BodyText(
                            text = stringResource(R.string.expense_review_payment_status),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(LABEL_WEIGHT)
                        )
                        Text(
                            text = it.displayText,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(VALUE_WEIGHT),
                            textAlign = TextAlign.End
                        )
                    }
                }
                if (uiState.formattedDueDate.isNotBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        BodyText(
                            text = stringResource(R.string.expense_review_due_date),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(LABEL_WEIGHT)
                        )
                        Text(
                            text = uiState.formattedDueDate,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(VALUE_WEIGHT),
                            textAlign = TextAlign.End
                        )
                    }
                }
                if (uiState.receiptUri != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        BodyText(
                            text = stringResource(R.string.expense_review_receipt),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(LABEL_WEIGHT)
                        )
                        Text(
                            text = stringResource(R.string.expense_review_receipt_attached),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(VALUE_WEIGHT),
                            textAlign = TextAlign.End
                        )
                    }
                }
            }

            // ── Split Section ────────────────────────────────────────────────────
            if (uiState.memberIds.size > 1) {
                uiState.selectedSplitType?.let {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        BodyText(
                            text = stringResource(R.string.expense_review_split_type),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(LABEL_WEIGHT)
                        )
                        Text(
                            text = it.displayText,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(VALUE_WEIGHT),
                            textAlign = TextAlign.End
                        )
                    }
                }
            }

            // ── Add-Ons Section ──────────────────────────────────────────────────
            if (uiState.addOns.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    BodyText(
                        text = stringResource(R.string.expense_review_add_ons),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(LABEL_WEIGHT)
                    )
                    Text(
                        text = uiState.addOns.size.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(VALUE_WEIGHT),
                        textAlign = TextAlign.End
                    )
                }
                if (uiState.effectiveTotal.isNotBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        BodyText(
                            text = stringResource(R.string.expense_review_effective_total),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(LABEL_WEIGHT)
                        )
                        Text(
                            text = uiState.effectiveTotal,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(VALUE_WEIGHT),
                            textAlign = TextAlign.End
                        )
                    }
                }
            }
        }
    }
}
