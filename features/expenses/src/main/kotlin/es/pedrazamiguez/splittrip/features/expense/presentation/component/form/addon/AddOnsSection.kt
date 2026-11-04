package es.pedrazamiguez.splittrip.features.expense.presentation.component.form.addon

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.extension.asString
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Plus
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.SectionHeadingText
import es.pedrazamiguez.splittrip.domain.enums.AddOnType
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.event.AddExpenseUiEvent
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.AddExpenseUiState

/**
 * Add-ons section of the Add Expense form.
 *
 * Displays all extras using [AddOnItemCard]s with progressive disclosure accordion,
 * an impact summary card displaying progressive breakdown calculations,
 * and an "Add" button for new add-ons.
 *
 * Stateless: takes [AddExpenseUiState] and emits [AddExpenseUiEvent]s.
 */
@Composable
fun AddOnsSection(
    uiState: AddExpenseUiState,
    onEvent: (AddExpenseUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val hasAddOns = uiState.addOns.isNotEmpty()
    val showCurrencySelector = uiState.availableCurrencies.size > 1

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium)
    ) {
        // Section Header
        SectionHeadingText(text = stringResource(R.string.add_expense_add_ons_title))

        // List of Extras
        if (hasAddOns) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)
            ) {
                uiState.addOns.forEach { addOn ->
                    AddOnItemCard(
                        addOn = addOn,
                        availableCurrencies = uiState.availableCurrencies,
                        paymentMethods = uiState.paymentMethods,
                        showCurrencySelector = showCurrencySelector,
                        onEvent = onEvent,
                        onRemove = { onEvent(AddExpenseUiEvent.AddOnRemoved(addOn.id)) },
                        onToggleExpand = { onEvent(AddExpenseUiEvent.AddOnExpandedToggled(addOn.id)) },
                        groupCurrency = uiState.groupCurrency
                    )
                }
            }
        }

        // Impact Summary Card
        if (uiState.addOns.any { it.resolvedAmountCents > 0L }) {
            AddOnsImpactSummaryCard(uiState = uiState)
        }

        // Validation Error
        uiState.addOnError?.let { errorUiText ->
            Text(
                text = errorUiText.asString(),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(MaterialTheme.spacing.Medium)
            )
        }

        // Add Button
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
