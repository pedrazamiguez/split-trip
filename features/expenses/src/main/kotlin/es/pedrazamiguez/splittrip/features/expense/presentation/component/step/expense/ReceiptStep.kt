package es.pedrazamiguez.splittrip.features.expense.presentation.component.step.expense

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.InfoCircle
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.PhotoAi
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.GradientButton
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.SecondaryButton
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.FlatCard
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.WizardStepLayout
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.component.form.receipt.ReceiptSection
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.event.AddExpenseUiEvent
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.AddExpenseUiState

/**
 * Step 11 (or Step 1 in AI mode): Receipt image attachment.
 */
@Composable
fun ReceiptStep(
    uiState: AddExpenseUiState,
    onEvent: (AddExpenseUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    WizardStepLayout(modifier = modifier) {
        Column(
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Large)
        ) {
            if (uiState.isAiCapable && uiState.isAiModeActive) {
                AiAutoFillCard()

                if (uiState.receiptUri == null) {
                    GradientButton(
                        text = stringResource(R.string.expense_autofill_ai_select_receipt),
                        onClick = { onEvent(AddExpenseUiEvent.RequestPickerSource) },
                        leadingIcon = TablerIcons.Outline.PhotoAi,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    ReceiptSection(
                        receiptUri = uiState.receiptUri,
                        mimeType = uiState.receiptAttachment?.mimeType,
                        onPickerRequested = { onEvent(AddExpenseUiEvent.RequestPickerSource) },
                        onRemoveImage = { onEvent(AddExpenseUiEvent.RemoveReceiptImage) },
                        onViewImage = { onEvent(AddExpenseUiEvent.ViewReceiptFullScreen) }
                    )
                }

                SecondaryButton(
                    text = stringResource(R.string.expense_autofill_switch_manual),
                    onClick = { onEvent(AddExpenseUiEvent.SetAiModeActive(false)) },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                ReceiptSection(
                    receiptUri = uiState.receiptUri,
                    mimeType = uiState.receiptAttachment?.mimeType,
                    onPickerRequested = { onEvent(AddExpenseUiEvent.RequestPickerSource) },
                    onRemoveImage = { onEvent(AddExpenseUiEvent.RemoveReceiptImage) },
                    onViewImage = { onEvent(AddExpenseUiEvent.ViewReceiptFullScreen) }
                )
            }
        }
    }
}

@Composable
private fun AiAutoFillCard(
    modifier: Modifier = Modifier
) {
    FlatCard(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.Default),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium)
        ) {
            AiAutoFillHeader()

            Text(
                text = stringResource(R.string.expense_autofill_ai_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            AiDisclaimerBox()
        }
    }
}

@Composable
private fun AiAutoFillHeader() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary
                        )
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = TablerIcons.Outline.PhotoAi,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(24.dp)
            )
        }
        Column {
            Text(
                text = stringResource(R.string.expense_autofill_ai_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.expense_autofill_ai_subtitle),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun AiDisclaimerBox() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = MaterialTheme.shapes.small
            )
            .padding(MaterialTheme.spacing.Small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)
    ) {
        Icon(
            imageVector = TablerIcons.Outline.InfoCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = stringResource(R.string.expense_autofill_ai_disclaimer),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
    }
}
