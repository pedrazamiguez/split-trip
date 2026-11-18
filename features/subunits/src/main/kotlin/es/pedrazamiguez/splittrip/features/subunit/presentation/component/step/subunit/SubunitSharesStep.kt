package es.pedrazamiguez.splittrip.features.subunit.presentation.component.step.subunit

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.FormErrorBanner
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.BodyText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.ShareAllocationInputRow
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.WizardStepLayout
import es.pedrazamiguez.splittrip.features.subunit.R
import es.pedrazamiguez.splittrip.features.subunit.presentation.viewmodel.event.CreateEditSubunitUiEvent
import es.pedrazamiguez.splittrip.features.subunit.presentation.viewmodel.state.CreateEditSubunitUiState

/**
 * Step 3: Allocate percentage shares for each selected member.
 *
 * Each member block shows the member name above a full-width percentage [ShareAllocationInputRow]
 * (with inline `"%"` unit and lock toggle), matching the split editor in `:features:expenses`.
 */
@Composable
fun SubunitSharesStep(
    uiState: CreateEditSubunitUiState,
    onEvent: (CreateEditSubunitUiEvent) -> Unit,
    onImeNext: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current

    WizardStepLayout(modifier = modifier) {
        FormErrorBanner(error = uiState.sharesError)

        val selectedMembers = uiState.availableMembers.filter { it.userId in uiState.selectedMemberIds }
        Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Default)) {
            selectedMembers.forEachIndexed { index, member ->
                val isLastRow = index == selectedMembers.lastIndex
                val isLocked = member.userId in uiState.lockedMemberIds

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)
                ) {
                    BodyText(
                        text = member.displayName,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    ShareAllocationInputRow(
                        value = uiState.memberShares[member.userId].orEmpty(),
                        onValueChange = { onEvent(CreateEditSubunitUiEvent.UpdateMemberShare(member.userId, it)) },
                        unitText = "%",
                        isLocked = isLocked,
                        lockContentDescription = stringResource(subunitLockDescriptionRes(isLocked)),
                        onLockToggle = { onEvent(CreateEditSubunitUiEvent.ToggleShareLock(member.userId)) },
                        fieldContentDescription = "%",
                        imeAction = if (isLastRow) ImeAction.Done else ImeAction.Next,
                        keyboardActions = shareKeyboardActions(isLastRow, focusManager, onImeNext)
                    )
                }
            }
        }
    }
}

@StringRes
private fun subunitLockDescriptionRes(isLocked: Boolean): Int =
    if (isLocked) R.string.subunit_share_unlock else R.string.subunit_share_lock

private fun shareKeyboardActions(
    isLastRow: Boolean,
    focusManager: FocusManager,
    onImeNext: () -> Unit
): KeyboardActions = if (isLastRow) {
    KeyboardActions(
        onDone = {
            focusManager.clearFocus()
            onImeNext()
        }
    )
} else {
    KeyboardActions.Default
}
