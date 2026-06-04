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
import es.pedrazamiguez.splittrip.features.subunit.presentation.viewmodel.event.CreateEditSubunitUiEvent
import es.pedrazamiguez.splittrip.features.subunit.presentation.viewmodel.state.CreateEditSubunitUiState

/**
 * Step 3: Allocate percentage shares for each selected member.
 *
 * Each row shows the member name, a percentage text field, and a lock toggle button.
 */
@Suppress("LongMethod", "CognitiveComplexMethod")
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
        Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium)) {
            selectedMembers.forEachIndexed { index, member ->
                val isLastRow = index == selectedMembers.lastIndex
                val shareText = uiState.memberShares[member.userId] ?: ""
                val isLocked = member.userId in uiState.lockedMemberIds

                Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.ExtraSmall)) {
                    BodyText(
                        text = member.displayName,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.ExtraSmall)
                    ) {
                        StyledOutlinedTextField(
                            value = shareText,
                            onValueChange = { onEvent(CreateEditSubunitUiEvent.UpdateMemberShare(member.userId, it)) },
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
                        IconButton(
                            onClick = { onEvent(CreateEditSubunitUiEvent.ToggleShareLock(member.userId)) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (isLocked) {
                                    TablerIcons.Filled.LockFilled
                                } else {
                                    TablerIcons.Outline.LockOpen
                                },
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
                }
            }
        }
    }
}
