package es.pedrazamiguez.splittrip.features.onboarding.presentation.preview

import androidx.compose.runtime.Composable
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewComplete
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewThemeWrapper
import es.pedrazamiguez.splittrip.features.onboarding.presentation.screen.ReconciliationScreen
import es.pedrazamiguez.splittrip.features.onboarding.presentation.viewmodel.state.ReconciliationUiState

@PreviewComplete
@Composable
fun ReconciliationScreenLoadingPreview() {
    PreviewThemeWrapper {
        ReconciliationScreen(
            uiState = ReconciliationUiState.Migrating,
            email = "user@example.com",
            onMigrateClick = {}
        )
    }
}

@PreviewComplete
@Composable
fun ReconciliationScreenWithDataPreview() {
    PreviewThemeWrapper {
        ReconciliationScreen(
            uiState = ReconciliationUiState.WaitingForYou,
            email = "user@example.com",
            onMigrateClick = {}
        )
    }
}

@PreviewComplete
@Composable
fun ReconciliationScreenEmptyPreview() {
    PreviewThemeWrapper {
        ReconciliationScreen(
            uiState = ReconciliationUiState.Success,
            email = "user@example.com",
            onMigrateClick = {}
        )
    }
}
