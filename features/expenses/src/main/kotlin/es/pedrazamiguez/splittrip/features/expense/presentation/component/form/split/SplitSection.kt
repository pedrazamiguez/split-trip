package es.pedrazamiguez.splittrip.features.expense.presentation.component.form.split

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.extension.asString
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.FlatCard
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.SectionHeadingText
import es.pedrazamiguez.splittrip.domain.enums.SplitType
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.event.AddExpenseUiEvent
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.AddExpenseUiState

/**
 * Complete split configuration section for the Add Expense form.
 * Combines the split type selector, per-user editor, and validation error display.
 */
@Composable
fun SplitSection(uiState: AddExpenseUiState, onEvent: (AddExpenseUiEvent) -> Unit, modifier: Modifier = Modifier) {
    val selectedSplitType = uiState.selectedSplitType
    val isEqualMode = selectedSplitType?.id == SplitType.EQUAL.name
    val isPercentMode = selectedSplitType?.id == SplitType.PERCENT.name

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium)
    ) {
        SectionHeadingText(text = stringResource(R.string.add_expense_split_title))

        SplitTypeSelector(
            splitTypes = uiState.availableSplitTypes,
            selectedSplitType = uiState.selectedSplitType,
            onSplitTypeSelected = { splitTypeId ->
                onEvent(AddExpenseUiEvent.SplitTypeChanged(splitTypeId))
            }
        )

        // Subunit mode toggle — only visible when the group has subunits
        if (uiState.hasSubunits) {
            SubunitModeToggle(
                isSubunitMode = uiState.isSubunitMode,
                onToggled = { onEvent(AddExpenseUiEvent.SubunitModeToggled) }
            )
        }

        if (uiState.isSubunitMode && uiState.entitySplits.isNotEmpty()) {
            // ── Subunit mode: Entity-level splits ───────────────
            SubunitModeSplitCard(
                uiState = uiState,
                isEqualMode = isEqualMode,
                isPercentMode = isPercentMode,
                onEvent = onEvent
            )
        } else if (uiState.splits.isNotEmpty()) {
            // ── Flat mode: Per-member splits ─────────────────────
            FlatModeSplitCard(
                uiState = uiState,
                isEqualMode = isEqualMode,
                isPercentMode = isPercentMode,
                onEvent = onEvent
            )
        }

        // Split validation error
        uiState.splitError?.let { errorUiText ->
            SplitValidationError(
                errorText = errorUiText.asString()
            )
        }
    }
}

@Composable
private fun SubunitModeSplitCard(
    uiState: AddExpenseUiState,
    isEqualMode: Boolean,
    isPercentMode: Boolean,
    onEvent: (AddExpenseUiEvent) -> Unit
) {
    EntitySplitEditor(
        entitySplits = uiState.entitySplits,
        isEqualMode = isEqualMode,
        isPercentMode = isPercentMode,
        availableSplitTypes = uiState.availableSplitTypes,
        events = EntitySplitEditorEvents(
            onAmountChanged = { entityId, amount ->
                onEvent(AddExpenseUiEvent.EntitySplitAmountChanged(entityId, amount))
            },
            onPercentageChanged = { entityId, percentage ->
                onEvent(AddExpenseUiEvent.EntitySplitPercentageChanged(entityId, percentage))
            },
            onExcludedToggled = { entityId ->
                onEvent(AddExpenseUiEvent.EntitySplitExcludedToggled(entityId))
            },
            onShareLockToggled = { entityId ->
                onEvent(AddExpenseUiEvent.EntityShareLockToggled(entityId))
            },
            onAccordionToggled = { entityId ->
                onEvent(AddExpenseUiEvent.EntityAccordionToggled(entityId))
            },
            onIntraSubunitSplitTypeChanged = { subunitId, splitTypeId ->
                onEvent(AddExpenseUiEvent.IntraSubunitSplitTypeChanged(subunitId, splitTypeId))
            },
            onIntraSubunitAmountChanged = { subunitId, userId, amount ->
                onEvent(AddExpenseUiEvent.IntraSubunitAmountChanged(subunitId, userId, amount))
            },
            onIntraSubunitPercentageChanged = { subunitId, userId, percentage ->
                onEvent(AddExpenseUiEvent.IntraSubunitPercentageChanged(subunitId, userId, percentage))
            },
            onIntraSubunitShareLockToggled = { subunitId, userId ->
                onEvent(AddExpenseUiEvent.IntraSubunitShareLockToggled(subunitId, userId))
            }
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun FlatModeSplitCard(
    uiState: AddExpenseUiState,
    isEqualMode: Boolean,
    isPercentMode: Boolean,
    onEvent: (AddExpenseUiEvent) -> Unit
) {
    SplitEditor(
        splits = uiState.splits,
        isEqualMode = isEqualMode,
        isPercentMode = isPercentMode,
        onAmountChanged = { userId, amount ->
            onEvent(AddExpenseUiEvent.SplitAmountChanged(userId, amount))
        },
        onPercentageChanged = { userId, percentage ->
            onEvent(AddExpenseUiEvent.SplitPercentageChanged(userId, percentage))
        },
        onExcludedToggled = { userId ->
            onEvent(AddExpenseUiEvent.SplitExcludedToggled(userId))
        },
        onShareLockToggled = { userId ->
            onEvent(AddExpenseUiEvent.SplitShareLockToggled(userId))
        },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun SplitValidationError(errorText: String) {
    FlatCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.errorContainer
    ) {
        Text(
            text = errorText,
            color = MaterialTheme.colorScheme.onErrorContainer,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(MaterialTheme.spacing.Medium)
        )
    }
}
