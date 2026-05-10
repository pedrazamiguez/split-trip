package es.pedrazamiguez.splittrip.features.authentication.presentation.screen

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import es.pedrazamiguez.splittrip.core.designsystem.extension.asString
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.navigation.DoubleTapBackToExitHandler
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.GradientButton
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.SecondaryButton
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.input.StyledOutlinedTextField
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.SecondaryBodyText
import es.pedrazamiguez.splittrip.features.authentication.R
import es.pedrazamiguez.splittrip.features.authentication.presentation.model.AuthenticationUiEvent
import es.pedrazamiguez.splittrip.features.authentication.presentation.model.AuthenticationUiState

/** Fraction of the screen width used by the login form. */
private const val FORM_WIDTH_FRACTION = 0.8f

@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    uiState: AuthenticationUiState,
    isGoogleSignInAvailable: Boolean = true,
    onEvent: (AuthenticationUiEvent) -> Unit = {},
    onGoogleSignInClick: () -> Unit = {},
    doubleTapBackHandler: DoubleTapBackToExitHandler = remember { DoubleTapBackToExitHandler() },
    navController: NavHostController = rememberNavController()
) {
    val activity = LocalActivity.current
    val anyLoading = uiState.isLoading || uiState.isGoogleLoading

    Scaffold { innerPadding ->
        Box(
            modifier = modifier.fillMaxSize().padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Default),
                modifier = Modifier.fillMaxWidth(FORM_WIDTH_FRACTION).verticalScroll(rememberScrollState()).imePadding()
            ) {
                LoginFormContent(
                    uiState = uiState,
                    anyLoading = anyLoading,
                    onEvent = onEvent
                )

                if (isGoogleSignInAvailable) {
                    GoogleSignInSection(
                        anyLoading = anyLoading,
                        isGoogleLoading = uiState.isGoogleLoading,
                        onGoogleSignInClick = onGoogleSignInClick
                    )
                }

                if (uiState.error != null) {
                    LoginErrorText(errorMessage = uiState.error.asString())
                }
            }
        }
    }

    BackHandler {
        val didPop = navController.popBackStack()
        if (!didPop && doubleTapBackHandler.shouldExit()) {
            activity?.finish()
        }
    }
}

@Composable
private fun LoginFormContent(
    uiState: AuthenticationUiState,
    anyLoading: Boolean,
    onEvent: (AuthenticationUiEvent) -> Unit
) {
    StyledOutlinedTextField(
        value = uiState.email,
        onValueChange = { onEvent(AuthenticationUiEvent.EmailChanged(it)) },
        label = stringResource(R.string.login_email_label),
        singleLine = true,
        enabled = !anyLoading,
        keyboardType = KeyboardType.Email,
        imeAction = ImeAction.Next,
        modifier = Modifier.fillMaxWidth()
    )
    StyledOutlinedTextField(
        value = uiState.password,
        onValueChange = { onEvent(AuthenticationUiEvent.PasswordChanged(it)) },
        label = stringResource(R.string.login_password_label),
        visualTransformation = PasswordVisualTransformation(),
        singleLine = true,
        enabled = !anyLoading,
        keyboardType = KeyboardType.Password,
        imeAction = ImeAction.Done,
        modifier = Modifier.fillMaxWidth()
    )
    GradientButton(
        text = stringResource(R.string.login_button),
        onClick = { onEvent(AuthenticationUiEvent.SubmitLogin) },
        enabled = !anyLoading,
        isLoading = uiState.isLoading,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun GoogleSignInSection(
    anyLoading: Boolean,
    isGoogleLoading: Boolean,
    onGoogleSignInClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium)
    ) {
        SecondaryBodyText(
            text = stringResource(R.string.login_or_divider),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = Int.MAX_VALUE
        )
        SecondaryButton(
            text = stringResource(R.string.login_google_button),
            onClick = onGoogleSignInClick,
            enabled = !anyLoading,
            isLoading = isGoogleLoading,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun LoginErrorText(errorMessage: String) {
    SecondaryBodyText(
        text = errorMessage,
        color = MaterialTheme.colorScheme.error,
        maxLines = Int.MAX_VALUE,
        modifier = Modifier.padding(top = MaterialTheme.spacing.Small)
    )
}
