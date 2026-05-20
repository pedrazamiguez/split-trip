package es.pedrazamiguez.splittrip.features.settings.presentation.feature

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.pedrazamiguez.splittrip.core.designsystem.navigation.Routes
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.scaffold.FeatureScaffold
import es.pedrazamiguez.splittrip.features.settings.presentation.screen.DeveloperServicesScreen
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.DeveloperServicesUiEvent
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.DeveloperServicesViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun DeveloperServicesFeature(
    viewModel: DeveloperServicesViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val name = getFileName(context, uri)
            val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
            viewModel.onEvent(DeveloperServicesUiEvent.FileSelected(uri.toString(), name, mimeType))
        }
    }

    FeatureScaffold(currentRoute = Routes.SETTINGS_DEVELOPER_SERVICES) {
        DeveloperServicesScreen(
            uiState = uiState,
            onSelectClick = {
                filePickerLauncher.launch(
                    arrayOf("image/jpeg", "image/png", "image/webp", "image/bmp", "application/pdf")
                )
            },
            onEvent = viewModel::onEvent
        )
    }
}

private fun getFileName(context: Context, uri: Uri): String {
    if (uri.scheme != "content") {
        return uri.path?.substringAfterLast('/') ?: "Unknown"
    }

    return context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index != -1) cursor.getString(index) else null
        } else {
            null
        }
    } ?: uri.path?.substringAfterLast('/') ?: "Unknown"
}
