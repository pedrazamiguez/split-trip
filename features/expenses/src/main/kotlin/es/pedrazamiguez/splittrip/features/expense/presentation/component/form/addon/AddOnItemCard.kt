package es.pedrazamiguez.splittrip.features.expense.presentation.component.form.addon

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.extension.debouncedClickable
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.ChevronDown
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.ChevronUp
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.X
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.FlatCard
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.AmountText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.CaptionText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.extensions.toStringRes
import es.pedrazamiguez.splittrip.core.designsystem.presentation.model.CurrencyUiModel
import es.pedrazamiguez.splittrip.domain.enums.AddOnType
import es.pedrazamiguez.splittrip.domain.enums.AddOnValueType
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.extensions.toStringRes
import es.pedrazamiguez.splittrip.features.expense.presentation.model.AddOnUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.model.PaymentMethodUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.event.AddExpenseUiEvent
import kotlinx.collections.immutable.ImmutableList

@Suppress("LongMethod", "LongParameterList", "CyclomaticComplexMethod", "CognitiveComplexMethod", "ComplexCondition")
@Composable
fun AddOnItemCard(
    addOn: AddOnUiModel,
    availableCurrencies: ImmutableList<CurrencyUiModel>,
    paymentMethods: ImmutableList<PaymentMethodUiModel>,
    showCurrencySelector: Boolean,
    onEvent: (AddExpenseUiEvent) -> Unit,
    onRemove: () -> Unit,
    onToggleExpand: () -> Unit,
    groupCurrency: CurrencyUiModel? = null,
    modifier: Modifier = Modifier
) {
    if (!addOn.isExpanded) {
        FlatCard(
            modifier = modifier
                .fillMaxWidth()
                .debouncedClickable { onToggleExpand() }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(MaterialTheme.spacing.Default),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.ExtraSmall)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(addOn.type.toStringRes()),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
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

                    if (addOn.description.isNotBlank()) {
                        CaptionText(
                            text = addOn.description,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    } else if (addOn.valueType == AddOnValueType.PERCENTAGE && addOn.amountInput.isNotBlank()) {
                        CaptionText(
                            text = "${addOn.amountInput}%",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val displayAmt = addOn.formattedAmount.ifBlank {
                        if (addOn.amountInput.isNotBlank()) {
                            "${addOn.amountInput} ${addOn.currency?.code.orEmpty()}".trim()
                        } else {
                            ""
                        }
                    }
                    if (displayAmt.isNotBlank()) {
                        val isDiscount = addOn.type == AddOnType.DISCOUNT
                        val valueColor = if (isDiscount) {
                            MaterialTheme.colorScheme.tertiary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                        val signedAmount = if (isDiscount) "− $displayAmt" else "+ $displayAmt"

                        Column(horizontalAlignment = Alignment.End) {
                            AmountText(text = signedAmount, color = valueColor)
                            val isForeignCurrency = addOn.showExchangeRateSection &&
                                addOn.currency != null &&
                                addOn.currency.code != groupCurrency?.code &&
                                addOn.amountInput.isNotBlank()
                            if (isForeignCurrency) {
                                CaptionText(
                                    text = "${addOn.amountInput} ${addOn.currency?.code.orEmpty()}",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    IconButton(onClick = onToggleExpand) {
                        Icon(
                            imageVector = TablerIcons.Outline.ChevronDown,
                            contentDescription = stringResource(R.string.add_expense_add_on_item_expand),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(onClick = onRemove) {
                        Icon(
                            imageVector = TablerIcons.Outline.X,
                            contentDescription = stringResource(R.string.add_expense_add_on_remove),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    } else {
        FlatCard(modifier = modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(MaterialTheme.spacing.Default),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(addOn.type.toStringRes()),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(onClick = onToggleExpand) {
                        Icon(
                            imageVector = TablerIcons.Outline.ChevronUp,
                            contentDescription = stringResource(R.string.add_expense_add_on_item_collapse),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(onClick = onRemove) {
                        Icon(
                            imageVector = TablerIcons.Outline.X,
                            contentDescription = stringResource(R.string.add_expense_add_on_remove),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                AddOnItemEditor(
                    addOn = addOn,
                    availableCurrencies = availableCurrencies,
                    paymentMethods = paymentMethods,
                    showCurrencySelector = showCurrencySelector,
                    onEvent = onEvent,
                    groupCurrency = groupCurrency
                )
            }
        }
    }
}
