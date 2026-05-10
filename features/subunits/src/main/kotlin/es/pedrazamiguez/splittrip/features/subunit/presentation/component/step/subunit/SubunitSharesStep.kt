package es.pedrazamiguez.splittrip.features.subunit.presentation.component.step.subunit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.filled.LockFilled
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.LockOpen
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.FormErrorBanner
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.input.StyledOutlinedTextField
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.BodyText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.WizardStepLayout
import es.pedrazamiguez.splittrip.features.subunit.R
import es.pedrazamiguez.splittrip.features.subunit.presentation.model.MemberUiModel
import es.pedrazamiguez.splittrip.features.subunit.presentation.viewmodel.event.CreateEditSubunitUiEvent
import es.pedrazamiguez.splittrip.features.subunit.presentation.viewmodel.state.CreateEditSubunitUiState

/**
 * Step 3: Allocate percentage shares for each selected member.
 *
 * Each row shows the member name, a percentage text field, and a lock toggle button.
 */
@Composable
fun SubunitSharesStep(
    uiState: CreateEditSubunitUiState,
    onEvent: (CreateEditSubunitUiEvent) -> Unit,
    onImeNext: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    WizardStepLayout(modifier = modifier) {
        FormErrorBanner(error = uiState.sharesError)

        ShareAllocationList(
            members = uiState.availableMembers,
            selectedMemberIds = uiState.selectedMemberIds,
            memberShares = uiState.memberShares,
            lockedMemberIds = uiState.lockedMemberIds,
            onShareChanged = { userId, share ->
                onEvent(CreateEditSubunitUiEvent.UpdateMemberShare(userId, share))
            },
            onShareLockToggled = { userId ->
                onEvent(CreateEditSubunitUiEvent.ToggleShareLock(userId))
            },
            onImeNext = onImeNext
        )
    }
}

/**
 * Bundled state for a single share allocation row.
 * Keeps the composable parameter count under the detekt threshold.
 */
internal data class ShareRowState(
    val memberId: String,
    val displayName: String,
    val shareText: String,
    val isLocked: Boolean
)

@Composable
private fun ShareAllocationList(
    members: kotlinx.collections.immutable.ImmutableList<MemberUiModel>,
    selectedMemberIds: kotlinx.collections.immutable.ImmutableList<String>,
    memberShares: Map<String, String>,
    lockedMemberIds: kotlinx.collections.immutable.ImmutableSet<String>,
    onShareChanged: (String, String) -> Unit,
    onShareLockToggled: (String) -> Unit,
    onImeNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedMembers = members.filter { it.userId in selectedMemberIds }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium)) {
        selectedMembers.forEachIndexed { index, member ->
            val isLastRow = index == selectedMembers.lastIndex
            val rowState = ShareRowState(
                memberId = member.userId,
                displayName = member.displayName,
                shareText = memberShares[member.userId] ?: "",
                isLocked = member.userId in lockedMemberIds
            )
            ShareInputRow(
                state = rowState,
                isLastRow = isLastRow,
                onShareChanged = { onShareChanged(member.userId, it) },
                onLockToggled = { onShareLockToggled(member.userId) },
                onImeNext = onImeNext
            )
        }
    }
}

@Composable
private fun ShareInputRow(
    state: ShareRowState,
    isLastRow: Boolean,
    onShareChanged: (String) -> Unit,
    onLockToggled: () -> Unit,
    onImeNext: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.ExtraSmall)) {
        BodyText(
            text = state.displayName,
            color = MaterialTheme.colorScheme.onSurface
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.ExtraSmall)
        ) {
            StyledOutlinedTextField(
                value = state.shareText,
                onValueChange = onShareChanged,
                label = "%",
                keyboardType = KeyboardType.Decimal,
                imeAction = if (isLastRow) ImeAction.Done else ImeAction.Next,
                keyboardActions = if (isLastRow) {
                    KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            onImeNext()
                        }
                    )
                } else {
                    KeyboardActions.Default
                },
                modifier = Modifier.weight(1f)
            )
            ShareLockButton(isLocked = state.isLocked, onClick = onLockToggled)
        }
    }
}

@Composable
private fun ShareLockButton(isLocked: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(36.dp)) {
        Icon(
            imageVector = if (isLocked) TablerIcons.Filled.LockFilled else TablerIcons.Outline.LockOpen,
            contentDescription = stringResource(
                if (isLocked) R.string.subunit_share_unlock else R.string.subunit_share_lock
            ),
            tint = if (isLocked) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            },
            modifier = Modifier.size(20.dp)
        )
    }
}
