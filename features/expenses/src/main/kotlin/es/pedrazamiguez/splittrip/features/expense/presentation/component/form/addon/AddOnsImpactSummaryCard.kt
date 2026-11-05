package es.pedrazamiguez.splittrip.features.expense.presentation.component.form.addon

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Receipt
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.FlatCard
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.AmountText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.LabelText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.SecondaryBodyText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.extensions.toStringRes
import es.pedrazamiguez.splittrip.domain.enums.AddOnMode
import es.pedrazamiguez.splittrip.domain.enums.AddOnType
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.extensions.toStringRes
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.AddExpenseUiState

/**
 * Summary card displaying the progressive breakdown of base cost, configured extras,
 * and the resulting effective total in the Add/Edit Expense wizard.
 */
@Suppress("LongMethod", "CyclomaticComplexMethod", "CognitiveComplexMethod")
@Composable
fun AddOnsImpactSummaryCard(
    uiState: AddExpenseUiState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = TablerIcons.Outline.Receipt,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            LabelText(text = stringResource(R.string.expense_detail_section_breakdown))
        }

        FlatCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(MaterialTheme.spacing.Default),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium)
            ) {
                // Base Cost Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val baseLabel = if (uiState.includedBaseCost.isNotBlank()) {
                        stringResource(R.string.expense_detail_breakdown_base_cost)
                    } else {
                        stringResource(R.string.add_expense_impact_entered_amount)
                    }
                    val baseAmount = if (uiState.includedBaseCost.isNotBlank()) {
                        uiState.includedBaseCost
                    } else {
                        val raw = uiState.calculatedGroupAmount.ifBlank { uiState.sourceAmount }
                        val code = uiState.groupCurrency?.code.orEmpty()
                        if (code.isNotBlank() && !raw.contains(code)) "$raw $code" else raw
                    }
                    Text(
                        text = baseLabel,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    AmountText(text = baseAmount)
                }

                // Configured Extras Rows
                val activeAddOns = uiState.addOns.filter { it.resolvedAmountCents > 0L }
                activeAddOns.forEach { addOn ->
                    val isDiscount = addOn.type == AddOnType.DISCOUNT
                    val valueColor = if (isDiscount) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                    val formatted = addOn.formattedAmount.ifBlank {
                        val code = addOn.currency?.code.orEmpty()
                        "${addOn.amountInput} $code".trim()
                    }
                    val signedAmount = if (isDiscount) "− $formatted" else "+ $formatted"

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SecondaryBodyText(text = stringResource(addOn.type.toStringRes()))
                            Surface(
                                shape = MaterialTheme.shapes.extraSmall,
                                color = MaterialTheme.colorScheme.surfaceContainerHigh
                            ) {
                                Text(
                                    text = stringResource(addOn.mode.toStringRes()),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        AmountText(text = signedAmount, color = valueColor)
                    }
                }

                // Resulting Total Row
                val hasOnTop = uiState.addOns.any { it.mode == AddOnMode.ON_TOP && it.resolvedAmountCents > 0L }
                val resultingTotalText = if (uiState.effectiveTotal.isNotBlank()) {
                    uiState.effectiveTotal
                } else if (uiState.includedBaseCost.isNotBlank()) {
                    val raw = uiState.calculatedGroupAmount.ifBlank { uiState.sourceAmount }
                    val code = uiState.groupCurrency?.code.orEmpty()
                    if (code.isNotBlank() && !raw.contains(code)) "$raw $code" else raw
                } else {
                    ""
                }

                if (resultingTotalText.isNotBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(
                                if (hasOnTop || uiState.effectiveTotal.isNotBlank()) {
                                    R.string.expense_review_effective_total
                                } else {
                                    R.string.expense_detail_breakdown_original_total
                                }
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        AmountText(
                            text = resultingTotalText,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
