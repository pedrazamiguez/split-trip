package es.pedrazamiguez.splittrip.features.settings.presentation.feature

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.pedrazamiguez.splittrip.core.designsystem.navigation.Routes
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.receipt.ReceiptAttachmentHandler
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.scaffold.FeatureScaffold
import es.pedrazamiguez.splittrip.features.settings.presentation.screen.DeveloperServicesScreen
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.DeveloperServicesTab
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.DeveloperServicesUiEvent
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.DeveloperServicesViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun DeveloperServicesFeature(
    viewModel: DeveloperServicesViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showReceiptSheet by remember { mutableStateOf(false) }
    // Track which tab triggered the sheet so we know what to do after file selection.
    var activeTabAtSheetOpen by remember { mutableStateOf<DeveloperServicesTab>(DeveloperServicesTab.Ocr) }

    ReceiptAttachmentHandler(
        showSheet = showReceiptSheet,
        onDismissSheet = { showReceiptSheet = false },
        onReceiptSelected = { uriString ->
            val uri = Uri.parse(uriString)
            val name = getFileName(context, uri)
            val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
            viewModel.onEvent(DeveloperServicesUiEvent.FileSelected(uriString, name, mimeType))
            if (activeTabAtSheetOpen is DeveloperServicesTab.AiExtraction) {
                viewModel.onEvent(DeveloperServicesUiEvent.RunOcrAndExtract)
            }
        }
    )

    FeatureScaffold(currentRoute = Routes.SETTINGS_DEVELOPER_SERVICES) {
        DeveloperServicesScreen(
            uiState = uiState,
            onSelectOcrFileClick = {
                activeTabAtSheetOpen = DeveloperServicesTab.Ocr
                showReceiptSheet = true
            },
            onSelectReceiptForAiClick = {
                activeTabAtSheetOpen = DeveloperServicesTab.AiExtraction
                showReceiptSheet = true
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
