package es.pedrazamiguez.splittrip.features.settings.presentation.feature

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.pedrazamiguez.splittrip.core.designsystem.navigation.Routes
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.scaffold.FeatureScaffold
import es.pedrazamiguez.splittrip.core.designsystem.presentation.notification.LocalTopPillController
import es.pedrazamiguez.splittrip.features.settings.R
import es.pedrazamiguez.splittrip.features.settings.presentation.screen.DeveloperInfoScreen
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.DeveloperInfoViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun DeveloperInfoFeature(
    viewModel: DeveloperInfoViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    val clipboardManager = LocalClipboardManager.current
    val pillController = LocalTopPillController.current
    val diagnosticsCopiedMsg = stringResource(R.string.developer_diagnostics_copied)

    FeatureScaffold(currentRoute = Routes.SETTINGS_DEVELOPER_INFO) {
        DeveloperInfoScreen(
            uiState = uiState,
            onLinkClick = { url ->
                if (url.isNotBlank()) {
                    uriHandler.openUri(url)
                }
            },
            onCopyDiagnosticsClick = {
                val logs = viewModel.getDiagnosticLogs()
                clipboardManager.setText(AnnotatedString(logs))
                pillController.showPill(diagnosticsCopiedMsg)
            }
        )
    }
}
