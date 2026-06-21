package es.pedrazamiguez.splittrip.features.authentication.presentation.screen

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.Brush
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.navigation.DoubleTapBackToExitHandler
import es.pedrazamiguez.splittrip.features.authentication.presentation.component.LoginFooter
import es.pedrazamiguez.splittrip.features.authentication.presentation.component.LoginFormCard
import es.pedrazamiguez.splittrip.features.authentication.presentation.component.LoginHeader
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
    onContinueAsGuestClick: () -> Unit = {},
    doubleTapBackHandler: DoubleTapBackToExitHandler = remember { DoubleTapBackToExitHandler() },
    navController: NavHostController = rememberNavController()
) {
    val activity = LocalActivity.current
    val anyLoading = uiState.isLoading || uiState.isGoogleLoading || uiState.isGuestLoading

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
                    onForgotPasswordClick = onForgotPasswordClick,
                    onContinueAsGuestClick = onContinueAsGuestClick
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
