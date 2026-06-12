package es.pedrazamiguez.splittrip.features.profile.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.common.presentation.asString
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalBottomPadding
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.FormSubmitButton
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.input.StyledOutlinedTextField
import es.pedrazamiguez.splittrip.features.profile.R
import es.pedrazamiguez.splittrip.features.profile.presentation.component.AvatarEditor
import es.pedrazamiguez.splittrip.features.profile.presentation.viewmodel.event.EditProfileUiEvent
import es.pedrazamiguez.splittrip.features.profile.presentation.viewmodel.state.EditProfileUiState

@Composable
fun EditProfileScreen(
    uiState: EditProfileUiState,
    onEvent: (EditProfileUiEvent) -> Unit,
    onAvatarClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val bottomPadding = LocalBottomPadding.current

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = MaterialTheme.spacing.ExtraLarge)
                .padding(
                    top = MaterialTheme.spacing.ExtraLarge,
                    bottom = 100.dp + bottomPadding
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.Medium))

            AvatarEditor(
                avatarUrl = uiState.avatarUrl,
                localAvatarPath = uiState.localAvatarPath,
                onClick = onAvatarClick
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.Section))

            StyledOutlinedTextField(
                value = uiState.displayName,
                onValueChange = { onEvent(EditProfileUiEvent.OnDisplayNameChanged(it)) },
                label = stringResource(R.string.edit_profile_display_name),
                placeholder = stringResource(R.string.edit_profile_display_name_placeholder),
                isError = uiState.displayNameError != null,
                supportingText = uiState.displayNameError?.asString(context),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.Large))

            StyledOutlinedTextField(
                value = uiState.bio,
                onValueChange = { onEvent(EditProfileUiEvent.OnBioChanged(it)) },
                label = stringResource(R.string.edit_profile_bio),
                placeholder = stringResource(R.string.edit_profile_bio_placeholder),
                singleLine = false,
                maxLines = 4,
                minLines = 3,
                isError = uiState.bioError != null,
                supportingText = uiState.bioError?.asString(context),
                modifier = Modifier.fillMaxWidth()
            )
        }

        FormSubmitButton(
            label = stringResource(R.string.edit_profile_save),
            isEnabled = !uiState.isSaving && !uiState.isLoading,
            isLoading = uiState.isSaving,
            onSubmit = { onEvent(EditProfileUiEvent.OnSaveClicked) },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
