package es.pedrazamiguez.splittrip.features.authentication.presentation.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import es.pedrazamiguez.splittrip.core.designsystem.R as DesignSystemR
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.ChevronDown
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.ChevronUp
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Mail
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.FormErrorBanner
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.GradientButton
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.SecondaryButton
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.FlatCard
import es.pedrazamiguez.splittrip.features.authentication.R
import es.pedrazamiguez.splittrip.features.authentication.presentation.model.AuthenticationUiEvent
import es.pedrazamiguez.splittrip.features.authentication.presentation.model.AuthenticationUiState

@Suppress("LongMethod")
@Composable
internal fun LoginFormCard(
    modifier: Modifier = Modifier,
    uiState: AuthenticationUiState,
    anyLoading: Boolean,
    isGoogleSignInAvailable: Boolean,
    onEvent: (AuthenticationUiEvent) -> Unit,
    onGoogleSignInClick: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    onContinueAsGuestClick: () -> Unit
) {
    val cardColor = MaterialTheme.colorScheme.surfaceContainer

    FlatCard(
        color = cardColor,
        shape = MaterialTheme.shapes.large,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.Large),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Default),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isGoogleSignInAvailable) {
                GradientButton(
                    text = stringResource(R.string.login_google_button),
                    onClick = onGoogleSignInClick,
                    enabled = !anyLoading,
                    isLoading = uiState.isGoogleLoading,
                    leadingIcon = ImageVector.vectorResource(
                        id = DesignSystemR.drawable.ic_google_logo
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                SecondaryButton(
                    text = stringResource(R.string.login_continue_with_email),
                    onClick = { onEvent(AuthenticationUiEvent.ToggleEmailForm) },
                    enabled = !anyLoading,
                    leadingIcon = TablerIcons.Outline.Mail,
                    trailingIcon = if (uiState.isEmailFormExpanded) {
                        TablerIcons.Outline.ChevronUp
                    } else {
                        TablerIcons.Outline.ChevronDown
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                AnimatedVisibility(
                    visible = uiState.isEmailFormExpanded,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Default),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        LoginFormFields(
                            uiState = uiState,
                            anyLoading = anyLoading,
                            onEvent = onEvent,
                            onForgotPasswordClick = onForgotPasswordClick
                        )

                        GradientButton(
                            text = stringResource(R.string.login_button),
                            onClick = { onEvent(AuthenticationUiEvent.SubmitLogin) },
                            enabled = !anyLoading,
                            isLoading = uiState.isLoading,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            } else {
                LoginFormFields(
                    uiState = uiState,
                    anyLoading = anyLoading,
                    onEvent = onEvent,
                    onForgotPasswordClick = onForgotPasswordClick
                )

                GradientButton(
                    text = stringResource(R.string.login_button),
                    onClick = { onEvent(AuthenticationUiEvent.SubmitLogin) },
                    enabled = !anyLoading,
                    isLoading = uiState.isLoading,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            OrDivider()

            SecondaryButton(
                text = stringResource(R.string.login_guest_button),
                onClick = onContinueAsGuestClick,
                enabled = !anyLoading,
                isLoading = uiState.isGuestLoading,
                modifier = Modifier.fillMaxWidth()
            )

            FormErrorBanner(error = uiState.error)
        }
    }
}
