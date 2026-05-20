package es.pedrazamiguez.splittrip.features.settings.presentation.screen

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.FormErrorBanner
import es.pedrazamiguez.splittrip.features.settings.R
import es.pedrazamiguez.splittrip.features.settings.presentation.component.ExtractedRawTextCard
import es.pedrazamiguez.splittrip.features.settings.presentation.component.ExtractedTextBlockCard
import es.pedrazamiguez.splittrip.features.settings.presentation.component.OcrOperationsCard
import es.pedrazamiguez.splittrip.features.settings.presentation.component.SelectedAttachmentCard
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.DeveloperServicesUiEvent
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.DeveloperServicesUiState
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.OcrStatus

@Composable
fun DeveloperServicesScreen(
    uiState: DeveloperServicesUiState,
    onEvent: (DeveloperServicesUiEvent) -> Unit,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val name = getFileName(context, uri)
            val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
            onEvent(DeveloperServicesUiEvent.FileSelected(uri.toString(), name, mimeType))
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(MaterialTheme.spacing.Large),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Large)
    ) {
        item {
            SelectedAttachmentCard(
                selectedFileUri = uiState.selectedFileUri,
                selectedFileName = uiState.selectedFileName,
                selectedFileMimeType = uiState.selectedFileMimeType,
                onSelectClick = { filePickerLauncher.launch(arrayOf("image/*", "application/pdf")) },
                onClearClick = { onEvent(DeveloperServicesUiEvent.Reset) }
            )
        }

        item {
            AnimatedVisibility(
                visible = uiState.selectedFileUri != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                OcrOperationsCard(
                    isLoading = uiState.ocrStatus is OcrStatus.Loading,
                    onRunOcrClick = { onEvent(DeveloperServicesUiEvent.RunOcr) }
                )
            }
        }

        item {
            AnimatedVisibility(
                visible = uiState.errorMessage != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                FormErrorBanner(error = uiState.errorMessage)
            }
        }

        item {
            AnimatedVisibility(
                visible = uiState.ocrStatus is OcrStatus.Success && uiState.extractedText.isNotEmpty(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                ExtractedRawTextCard(extractedText = uiState.extractedText)
            }
        }

        if (uiState.ocrStatus is OcrStatus.Success && uiState.textBlocks.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.developer_services_extracted_blocks, uiState.textBlocks.size),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = MaterialTheme.spacing.Small)
                )
            }

            itemsIndexed(uiState.textBlocks) { index, blockText ->
                ExtractedTextBlockCard(
                    blockIndex = index + 1,
                    blockText = blockText,
                    modifier = Modifier.padding(bottom = MaterialTheme.spacing.Small)
                )
            }
        }
    }
}

private fun getFileName(context: Context, uri: Uri): String {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        try {
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    result = cursor.getString(index)
                }
            }
        } finally {
            cursor?.close()
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/')
        if (cut != null && cut != -1) {
            result = result.substring(cut + 1)
        }
    }
    return result ?: "Unknown"
}
