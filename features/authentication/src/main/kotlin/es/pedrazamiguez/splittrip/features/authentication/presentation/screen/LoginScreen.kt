package es.pedrazamiguez.splittrip.features.authentication.presentation.screen

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import es.pedrazamiguez.splittrip.core.designsystem.R as DesignSystemR
import es.pedrazamiguez.splittrip.core.designsystem.extension.asString
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.navigation.DoubleTapBackToExitHandler
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.GradientButton
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.SecondaryButton
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.input.StyledOutlinedTextField
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.FlatCard
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.ScreenTitleText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.SecondaryBodyText
import es.pedrazamiguez.splittrip.features.authentication.R
import es.pedrazamiguez.splittrip.features.authentication.presentation.model.AuthenticationUiEvent
import es.pedrazamiguez.splittrip.features.authentication.presentation.model.AuthenticationUiState

/** Fraction of the screen width used by the login form. */
private const val FORM_WIDTH_FRACTION = 0.85f

@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    uiState: AuthenticationUiState,
    isGoogleSignInAvailable: Boolean = true,
    onEvent: (AuthenticationUiEvent) -> Unit = {},
    onGoogleSignInClick: () -> Unit = {},
    onForgotPasswordClick: () -> Unit = {},
    onStartJourneyClick: () -> Unit = {},
    doubleTapBackHandler: DoubleTapBackToExitHandler = remember { DoubleTapBackToExitHandler() },
    navController: NavHostController = rememberNavController()
) {
    val activity = LocalActivity.current
    val anyLoading = uiState.isLoading || uiState.isGoogleLoading

    // Page background gradient
    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.05f),
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.05f)
        )
    )

    Scaffold { innerPadding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(backgroundGradient)
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth(FORM_WIDTH_FRACTION)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = MaterialTheme.spacing.Large)
                    .imePadding()
            ) {
                // Header section
                LoginHeader()

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.Section))

                LoginFormCard(
                    uiState = uiState,
                    anyLoading = anyLoading,
                    isGoogleSignInAvailable = isGoogleSignInAvailable,
                    onEvent = onEvent,
                    onGoogleSignInClick = onGoogleSignInClick,
                    onForgotPasswordClick = onForgotPasswordClick
                )

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.Large))

                // Footer section
                LoginFooter(onStartJourneyClick = onStartJourneyClick)
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
private fun LoginHeader() {
    val isDark = isSystemInDarkTheme()
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .shadow(
                    elevation = if (isDark) 0.dp else 8.dp,
                    shape = CircleShape
                )
                .background(
                    color = if (isDark) {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    } else {
                        Color.White
                    },
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = DesignSystemR.drawable.ic_brand_logo),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(56.dp)
            )
        }

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.ExtraSmall))

        ScreenTitleText(
            text = stringResource(id = DesignSystemR.string.app_name),
            color = MaterialTheme.colorScheme.onBackground
        )

        SecondaryBodyText(
            text = stringResource(id = R.string.login_welcome),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun LoginFormCard(
    uiState: AuthenticationUiState,
    anyLoading: Boolean,
    isGoogleSignInAvailable: Boolean,
    onEvent: (AuthenticationUiEvent) -> Unit,
    onGoogleSignInClick: () -> Unit,
    onForgotPasswordClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val cardColor = if (isDark) {
        MaterialTheme.colorScheme.surfaceContainerHighest
    } else {
        MaterialTheme.colorScheme.surfaceContainerLowest
    }

    FlatCard(
        color = cardColor,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.Large),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Default),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LoginFormFields(
                uiState = uiState,
                anyLoading = anyLoading,
                onEvent = onEvent,
                onForgotPasswordClick = onForgotPasswordClick
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.Small))

            GradientButton(
                text = stringResource(R.string.login_button),
                onClick = { onEvent(AuthenticationUiEvent.SubmitLogin) },
                enabled = !anyLoading,
                isLoading = uiState.isLoading,
                modifier = Modifier.fillMaxWidth()
            )

            if (isGoogleSignInAvailable) {
                OrDivider()

                SecondaryButton(
                    text = stringResource(R.string.login_google_button),
                    onClick = onGoogleSignInClick,
                    enabled = !anyLoading,
                    isLoading = uiState.isGoogleLoading,
                    leadingIcon = ImageVector.vectorResource(
                        id = DesignSystemR.drawable.ic_google_logo
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (uiState.error != null) {
                LoginErrorText(errorMessage = uiState.error.asString())
            }
        }
    }
}

@Composable
private fun LoginFormFields(
    uiState: AuthenticationUiState,
    anyLoading: Boolean,
    onEvent: (AuthenticationUiEvent) -> Unit,
    onForgotPasswordClick: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Default),
        modifier = Modifier.fillMaxWidth()
    ) {
        StyledOutlinedTextField(
            value = uiState.email,
            onValueChange = { onEvent(AuthenticationUiEvent.EmailChanged(it)) },
            placeholder = stringResource(R.string.login_email_label),
            singleLine = true,
            enabled = !anyLoading,
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next,
            shape = CircleShape,
            modifier = Modifier.fillMaxWidth()
        )
        StyledOutlinedTextField(
            value = uiState.password,
            onValueChange = { onEvent(AuthenticationUiEvent.PasswordChanged(it)) },
            placeholder = stringResource(R.string.login_password_label),
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            enabled = !anyLoading,
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done,
            shape = CircleShape,
            modifier = Modifier.fillMaxWidth()
        )
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.CenterEnd
        ) {
            Text(
                text = stringResource(R.string.login_forgot_password),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(
                        enabled = !anyLoading,
                        onClick = onForgotPasswordClick
                    )
                    .padding(
                        horizontal = MaterialTheme.spacing.Small,
                        vertical = MaterialTheme.spacing.ExtraSmall
                    )
            )
        }
    }
}

@Composable
private fun OrDivider() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = MaterialTheme.spacing.Small)
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
        )
        Text(
            text = stringResource(R.string.login_or_divider).uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = MaterialTheme.spacing.Medium)
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
        )
    }
}

@Composable
private fun LoginFooter(onStartJourneyClick: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        SecondaryBodyText(
            text = stringResource(R.string.login_new_explorer) + " ",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(R.string.login_start_journey),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .clip(CircleShape)
                .clickable(onClick = onStartJourneyClick)
                .padding(
                    horizontal = MaterialTheme.spacing.Small,
                    vertical = MaterialTheme.spacing.ExtraSmall
                )
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
