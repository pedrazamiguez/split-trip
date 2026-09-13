package es.pedrazamiguez.splittrip.features.onboarding.presentation.screen

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import es.pedrazamiguez.splittrip.core.designsystem.navigation.DoubleTapBackToExitHandler
import es.pedrazamiguez.splittrip.features.onboarding.presentation.component.OnboardingContent
import es.pedrazamiguez.splittrip.features.onboarding.presentation.viewmodel.state.OnboardingUiState

@Composable
fun OnboardingScreen(
    uiState: OnboardingUiState = OnboardingUiState(),
    onNextClick: () -> Unit = {},
    onPreviousClick: () -> Unit = {},
    onSkipClick: () -> Unit = {},
    onCompleteClick: () -> Unit = {},
    doubleTapBackHandler: DoubleTapBackToExitHandler = remember { DoubleTapBackToExitHandler() },
    modifier: Modifier = Modifier
) {
    val activity = LocalActivity.current

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        OnboardingContent(
            uiState = uiState,
            onNextClick = onNextClick,
            onPreviousClick = onPreviousClick,
            onSkipClick = onSkipClick,
            onCompleteClick = onCompleteClick,
            modifier = Modifier.padding(innerPadding)
        )
    }

    BackHandler {
        if (!uiState.isFirstStep) {
            onPreviousClick()
        } else if (doubleTapBackHandler.shouldExit()) {
            activity?.finish()
        }
    }
}
