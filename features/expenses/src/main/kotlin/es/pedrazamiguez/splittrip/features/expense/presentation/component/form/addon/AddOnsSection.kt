package es.pedrazamiguez.splittrip.features.expense.presentation.component.form.addon

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.extension.asString
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.ChevronDown
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.ChevronUp
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Plus
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.SectionHeadingText
import es.pedrazamiguez.splittrip.domain.enums.AddOnType
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.event.AddExpenseUiEvent
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.AddExpenseUiState

/**
 * Add-ons section of the Add Expense form.
 *
 * Displays a collapsible section with individual add-on editors,
 * an "Add" button for new add-ons, the effective total display,
 * and any add-on validation error.
 *
 * Stateless: takes [AddExpenseUiState] and emits [AddExpenseUiEvent]s.
 */
@Suppress("LongMethod", "CyclomaticComplexMethod", "CognitiveComplexMethod")
@Composable
fun AddOnsSection(
    uiState: AddExpenseUiState,
    onEvent: (AddExpenseUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val hasAddOns = uiState.addOns.isNotEmpty()
    val isExpanded = uiState.isAddOnsSectionExpanded

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium)
    ) {
        // Header (previously AddOnsSectionHeader)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionHeadingText(text = stringResource(R.string.add_expense_add_ons_title))

            if (hasAddOns) {
                TextButton(onClick = { onEvent(AddExpenseUiEvent.AddOnsSectionToggled) }) {
                    Icon(
                        imageVector = if (isExpanded) {
                            TablerIcons.Outline.ChevronUp
                        } else {
                            TablerIcons.Outline.ChevronDown
                        },
                        contentDescription = stringResource(
                            if (isExpanded) {
                                R.string.add_expense_add_ons_collapse
                            } else {
                                R.string.add_expense_add_ons_expand
                            }
                        ),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // List Card (previously AddOnsListCard)
        val showCurrencySelector = uiState.availableCurrencies.size > 1
        AnimatedVisibility(
            visible = isExpanded && hasAddOns,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)
            ) {
                uiState.addOns.forEach { addOn ->
                    AddOnItemEditor(
                        addOn = addOn,
                        availableCurrencies = uiState.availableCurrencies,
                        paymentMethods = uiState.paymentMethods,
                        showCurrencySelector = showCurrencySelector,
                        onEvent = { event -> onEvent(event) },
                        onRemove = {
                            onEvent(AddExpenseUiEvent.AddOnRemoved(addOn.id))
                        }
                    )
                }
            }
        }

        // Footer (previously AddOnsSectionFooter)
        if (uiState.effectiveTotal.isNotBlank()) {
            Text(
                text = stringResource(
                    R.string.add_expense_add_on_effective_total,
                    uiState.effectiveTotal
                ),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (uiState.includedBaseCost.isNotBlank()) {
            Text(
                text = stringResource(
                    R.string.add_expense_add_on_base_cost,
                    uiState.includedBaseCost
                ),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        uiState.addOnError?.let { errorUiText ->
            Text(
                text = errorUiText.asString(),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(MaterialTheme.spacing.Medium)
            )
        }

        TextButton(
            onClick = {
                focusManager.clearFocus()
                onEvent(AddExpenseUiEvent.AddOnAdded(AddOnType.FEE))
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = TablerIcons.Outline.Plus,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.size(MaterialTheme.spacing.ExtraSmall))
            Text(
                text = stringResource(R.string.add_expense_add_on_button),
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}
