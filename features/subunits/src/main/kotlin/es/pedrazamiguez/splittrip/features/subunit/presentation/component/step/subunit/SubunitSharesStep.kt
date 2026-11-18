package es.pedrazamiguez.splittrip.features.subunit.presentation.component.step.subunit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.constant.UiConstants
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.FormErrorBanner
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.input.StyledOutlinedTextField
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.BodyText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.ShareLockButton
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.WizardStepLayout
import es.pedrazamiguez.splittrip.features.subunit.R
import es.pedrazamiguez.splittrip.features.subunit.presentation.viewmodel.event.CreateEditSubunitUiEvent
import es.pedrazamiguez.splittrip.features.subunit.presentation.viewmodel.state.CreateEditSubunitUiState

private val STANDARD_PERCENT_MAX_WIDTH = 100.dp
private val NARROW_PERCENT_MAX_WIDTH = 84.dp

/**
 * Step 3: Allocate percentage shares for each selected member.
 *
 * Each row shows the member name on the left and a constrained percentage text field +
 * shared [ShareLockButton] on the right, matching `SplitMemberRow` in `:features:expenses`.
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
    val isNarrowScreen = LocalConfiguration.current.screenWidthDp.dp <= UiConstants.NARROW_SCREEN_BREAKPOINT
    val percentMaxWidth = if (isNarrowScreen) NARROW_PERCENT_MAX_WIDTH else STANDARD_PERCENT_MAX_WIDTH

    WizardStepLayout(modifier = modifier) {
        FormErrorBanner(error = uiState.sharesError)

        val selectedMembers = uiState.availableMembers.filter { it.userId in uiState.selectedMemberIds }
        Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium)) {
            selectedMembers.forEachIndexed { index, member ->
                val isLastRow = index == selectedMembers.lastIndex
                val shareText = uiState.memberShares[member.userId] ?: ""
                val isLocked = member.userId in uiState.lockedMemberIds
                val lockDescription = stringResource(
                    if (isLocked) R.string.subunit_share_unlock else R.string.subunit_share_lock
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = MaterialTheme.spacing.Small),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(
                        if (isNarrowScreen) MaterialTheme.spacing.ExtraSmall else MaterialTheme.spacing.Small
                    )
                ) {
                    BodyText(
                        text = member.displayName,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Row(
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
                            modifier = Modifier.widthIn(max = percentMaxWidth)
                        )
                        ShareLockButton(
                            isLocked = isLocked,
                            contentDescription = lockDescription,
                            onClick = { onEvent(CreateEditSubunitUiEvent.ToggleShareLock(member.userId)) }
                        )
                    }
                }
            }
        }
    }
}
