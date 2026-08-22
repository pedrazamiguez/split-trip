package es.pedrazamiguez.splittrip.features.expense.presentation.component.form.subexpense

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
import androidx.compose.material3.Switch
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
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Plus
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.SectionHeadingText
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.event.AddExpenseUiEvent
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.AddExpenseUiState

@Suppress("LongMethod", "CyclomaticComplexMethod")
@Composable
fun SubExpensesSection(
    uiState: AddExpenseUiState,
    onEvent: (AddExpenseUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium)
    ) {
        // Toggle Switch Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                SectionHeadingText(text = stringResource(R.string.expense_sub_expenses_title))
                Text(
                    text = stringResource(R.string.expense_sub_expenses_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Switch(
                checked = uiState.isSubExpensesEnabled,
                onCheckedChange = { onEvent(AddExpenseUiEvent.SubExpensesToggled) }
            )
        }

        // Sub-expenses list & editors
        AnimatedVisibility(
            visible = uiState.isSubExpensesEnabled,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)
            ) {
                // Balance allocation summary
                if (uiState.subExpensesAllocatedFormatted.isNotBlank() ||
                    uiState.subExpensesRemainingFormatted.isNotBlank()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(
                                R.string.expense_sub_expense_allocated,
                                uiState.subExpensesAllocatedFormatted
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(
                                R.string.expense_sub_expense_remaining,
                                uiState.subExpensesRemainingFormatted
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (uiState.subExpensesError != null) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.primary
                            }
                        )
                    }
                }

                // Tranche item editors
                uiState.subExpenses.forEachIndexed { index, subExpense ->
                    SubExpenseItemEditor(
                        subExpense = subExpense,
                        itemIndex = index,
                        paymentMethods = uiState.paymentMethods,
                        onEvent = onEvent,
                        onRemove = { onEvent(AddExpenseUiEvent.SubExpenseRemoved(subExpense.id)) }
                    )
                }

                // Error message
                uiState.subExpensesError?.let { errorUiText ->
                    Text(
                        text = errorUiText.asString(),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = MaterialTheme.spacing.Small)
                    )
                }

                // Add Tranche button
                TextButton(
                    onClick = {
                        focusManager.clearFocus()
                        onEvent(AddExpenseUiEvent.SubExpenseAdded)
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
                        text = stringResource(R.string.expense_sub_expense_add),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}
