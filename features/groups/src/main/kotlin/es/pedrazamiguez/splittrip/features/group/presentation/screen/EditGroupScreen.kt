package es.pedrazamiguez.splittrip.features.group.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.constant.UiConstants
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalBottomPadding
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.FormSubmitButton
import es.pedrazamiguez.splittrip.features.group.R
import es.pedrazamiguez.splittrip.features.group.presentation.component.EditGroupFieldsSection
import es.pedrazamiguez.splittrip.features.group.presentation.component.EditGroupImageSection
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.event.EditGroupUiEvent
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.state.EditGroupUiState

@Composable
fun EditGroupScreen(
    uiState: EditGroupUiState,
    onEvent: (EditGroupUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val bottomPadding = LocalBottomPadding.current

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = MaterialTheme.spacing.ExtraLarge)
                .padding(
                    top = MaterialTheme.spacing.ExtraLarge,
                    bottom = UiConstants.FORM_SUBMIT_BUTTON_HEIGHT + bottomPadding
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            EditGroupImageSection(uiState = uiState, onEvent = onEvent)

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.Large))

            EditGroupFieldsSection(uiState = uiState, onEvent = onEvent)
        }

        FormSubmitButton(
            label = stringResource(R.string.group_edit_save),
            isEnabled = !uiState.isSaving && !uiState.isLoading,
            isLoading = uiState.isSaving,
            onSubmit = { onEvent(EditGroupUiEvent.SaveClicked) },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
