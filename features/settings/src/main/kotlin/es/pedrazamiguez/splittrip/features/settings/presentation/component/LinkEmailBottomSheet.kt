package es.pedrazamiguez.splittrip.features.settings.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.core.designsystem.extension.asString
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Lock
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.GradientButton
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.SecondaryButton
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.input.PasswordRequirementsIndicator
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.input.StyledOutlinedTextField
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.SheetTitleText
import es.pedrazamiguez.splittrip.features.settings.R
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.event.AccountStatusUiEvent
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.state.AccountStatusUiState

@Suppress("LongMethod", "CognitiveComplexMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LinkEmailBottomSheet(
    uiState: AccountStatusUiState,
    onEvent: (AccountStatusUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val isMismatch = uiState.linkPasswordError ==
        UiText.StringResource(R.string.account_status_link_email_sheet_error_mismatch)
    val isEmailEmptyError = uiState.linkPasswordError ==
        UiText.StringResource(R.string.account_status_link_email_sheet_error_email_empty)
    val isPasswordError = uiState.linkPasswordError != null && !isMismatch && !isEmailEmptyError

    ModalBottomSheet(
        onDismissRequest = { onEvent(AccountStatusUiEvent.DismissLinkEmailSheet) },
        dragHandle = null,
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = { WindowInsets.safeDrawing },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = MaterialTheme.spacing.Default,
                    end = MaterialTheme.spacing.Default,
                    top = MaterialTheme.spacing.ExtraLarge,
                    bottom = MaterialTheme.spacing.ExtraLarge
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = TablerIcons.Outline.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(28.dp)
                )
            }

            SheetTitleText(text = stringResource(R.string.account_status_link_email_sheet_title))

            if (uiState.isAnonymous) {
                StyledOutlinedTextField(
                    label = stringResource(R.string.account_status_link_email_sheet_email),
                    value = uiState.linkEmailInput,
                    onValueChange = { onEvent(AccountStatusUiEvent.LinkEmailChanged(it)) },
                    enabled = !uiState.isLinking,
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                    isError = isEmailEmptyError,
                    supportingText = if (isEmailEmptyError) uiState.linkPasswordError.asString() else null,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            StyledOutlinedTextField(
                label = stringResource(R.string.account_status_link_email_sheet_password),
                value = uiState.linkPasswordInput,
                onValueChange = { onEvent(AccountStatusUiEvent.LinkPasswordChanged(it)) },
                visualTransformation = PasswordVisualTransformation(),
                enabled = !uiState.isLinking,
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next,
                isError = isPasswordError,
                supportingText = if (isPasswordError) uiState.linkPasswordError.asString() else null,
                modifier = Modifier.fillMaxWidth()
            )

            PasswordRequirementsIndicator(
                status = uiState.linkPasswordRequirementStatus,
                modifier = Modifier.fillMaxWidth()
            )

            StyledOutlinedTextField(
                label = stringResource(R.string.account_status_link_email_sheet_confirm_password),
                value = uiState.linkConfirmPasswordInput,
                onValueChange = { onEvent(AccountStatusUiEvent.LinkConfirmPasswordChanged(it)) },
                visualTransformation = PasswordVisualTransformation(),
                enabled = !uiState.isLinking,
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
                isError = isMismatch,
                supportingText = if (isMismatch) uiState.linkPasswordError.asString() else null,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)
            ) {
                SecondaryButton(
                    text = stringResource(R.string.account_status_link_email_sheet_cancel),
                    onClick = { onEvent(AccountStatusUiEvent.DismissLinkEmailSheet) },
                    enabled = !uiState.isLinking,
                    modifier = Modifier.weight(1f)
                )
                GradientButton(
                    text = stringResource(R.string.account_status_link_email_sheet_confirm),
                    onClick = { onEvent(AccountStatusUiEvent.SubmitLinkEmailPassword) },
                    enabled = !uiState.isLinking,
                    isLoading = uiState.isLinking,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
