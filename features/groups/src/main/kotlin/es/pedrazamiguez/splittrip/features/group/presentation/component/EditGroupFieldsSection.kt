package es.pedrazamiguez.splittrip.features.group.presentation.component

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.input.StyledOutlinedTextField
import es.pedrazamiguez.splittrip.features.group.R
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.event.EditGroupUiEvent
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.state.EditGroupUiState

@Composable
internal fun EditGroupFieldsSection(
    uiState: EditGroupUiState,
    onEvent: (EditGroupUiEvent) -> Unit
) {
    StyledOutlinedTextField(
        value = uiState.groupName,
        onValueChange = { onEvent(EditGroupUiEvent.NameChanged(it)) },
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
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(MaterialTheme.spacing.Large))

    StyledOutlinedTextField(
        value = uiState.groupDescription,
        onValueChange = { onEvent(EditGroupUiEvent.DescriptionChanged(it)) },
        label = stringResource(R.string.group_field_description),
        singleLine = false,
        maxLines = 4,
        keyboardType = KeyboardType.Text,
        imeAction = ImeAction.Next,
        capitalization = KeyboardCapitalization.Sentences,
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(MaterialTheme.spacing.Large))

    EditGroupCurrenciesSection(uiState = uiState, onEvent = onEvent)
}
