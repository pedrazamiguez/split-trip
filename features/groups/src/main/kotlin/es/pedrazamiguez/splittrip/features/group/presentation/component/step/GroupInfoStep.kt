package es.pedrazamiguez.splittrip.features.group.presentation.component.step

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.input.StyledOutlinedTextField
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.input.rememberAutoFocusRequester
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.WizardStepLayout
import es.pedrazamiguez.splittrip.features.group.R
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.event.CreateEditGroupUiEvent
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.state.CreateEditGroupUiState

/**
 * Step 1: Group name + optional description.
 */
@Composable
fun GroupInfoStep(
    uiState: CreateEditGroupUiState,
    onEvent: (CreateEditGroupUiEvent) -> Unit,
    onImeNext: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val nameFocusRequester = rememberAutoFocusRequester()

    WizardStepLayout(modifier = modifier) {
        StyledOutlinedTextField(
            value = uiState.groupName,
            onValueChange = { onEvent(CreateEditGroupUiEvent.NameChanged(it)) },
            label = stringResource(R.string.group_field_name),
            isError = !uiState.isNameValid,
            supportingText = if (!uiState.isNameValid) {
                stringResource(R.string.group_field_name_required)
            } else {
                null
            },
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Next,
            capitalization = KeyboardCapitalization.Sentences,
            modifier = Modifier.fillMaxWidth(),
            focusRequester = nameFocusRequester,
            moveCursorToEndOnFocus = true
        )
        StyledOutlinedTextField(
            value = uiState.groupDescription,
            onValueChange = { onEvent(CreateEditGroupUiEvent.DescriptionChanged(it)) },
            label = stringResource(R.string.group_field_description),
            singleLine = false,
            maxLines = 4,
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Done,
            capitalization = KeyboardCapitalization.Sentences,
            modifier = Modifier.fillMaxWidth(),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    onImeNext()
                }
            )
        )
    }
}
