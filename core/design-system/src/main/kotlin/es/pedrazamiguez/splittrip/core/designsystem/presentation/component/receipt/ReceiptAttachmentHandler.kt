package es.pedrazamiguez.splittrip.core.designsystem.presentation.component.receipt

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.FileProvider
import es.pedrazamiguez.splittrip.core.designsystem.R
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Camera
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Inbox
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Photo
import es.pedrazamiguez.splittrip.core.designsystem.permission.rememberRequestCameraPermission
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.sheet.ActionBottomSheet
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.sheet.SheetAction
import es.pedrazamiguez.splittrip.core.designsystem.presentation.notification.LocalTopPillController
import java.io.File

/**
 * Headless composable that owns the activity-result launchers for camera, gallery, and document
 * picking. Shows [ReceiptSourceSelectionSheet] when [showSheet] is true, then dispatches the
 * chosen URI string to [onReceiptSelected].
 *
 * Intended for use in Feature-layer composables to keep launcher registration out of the
 * ViewModel and off the Screen (stateless renderer).
 */
@Composable
fun ReceiptAttachmentHandler(
    showSheet: Boolean,
    onDismissSheet: () -> Unit,
    onReceiptSelected: (String) -> Unit
) {
    val context = LocalContext.current
    val pillController = LocalTopPillController.current
    val cameraPermissionRequiredMessage = stringResource(R.string.camera_permission_required)

    var cameraTempFile by remember { mutableStateOf<File?>(null) }
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { captured ->
        if (captured) {
            cameraImageUri?.let { uri -> onReceiptSelected(uri.toString()) }
        } else {
            // Discard the temp .jpg if the user cancelled or capture failed.
            // On success the file is cleaned up by ReceiptStorageServiceImpl after compression.
            cameraTempFile?.delete()
        }
        cameraTempFile = null
        cameraImageUri = null
    }

    val requestCameraPermission = rememberRequestCameraPermission { isGranted ->
        if (isGranted) {
            val (tempFile, uri) = createReceiptCameraUri(context)
            cameraTempFile = tempFile
            cameraImageUri = uri
            cameraLauncher.launch(uri)
        } else {
            pillController.showPill(cameraPermissionRequiredMessage)
        }
    }

    // Uses the Android 13+ photo picker to avoid requesting READ_MEDIA_IMAGES permission.
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { onReceiptSelected(it.toString()) }
    }

    val documentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { onReceiptSelected(it.toString()) }
    }

    if (showSheet) {
        ReceiptSourceSelectionSheet(
            onCameraSelected = {
                onDismissSheet()
                requestCameraPermission()
            },
            onGallerySelected = {
                onDismissSheet()
                galleryLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onDocumentSelected = {
                onDismissSheet()
                documentLauncher.launch(arrayOf("image/*", "application/pdf"))
            },
            onDismiss = onDismissSheet
        )
    }
}

@Composable
fun ReceiptSourceSelectionSheet(
    onCameraSelected: () -> Unit,
    onGallerySelected: () -> Unit,
    onDocumentSelected: () -> Unit,
    onDismiss: () -> Unit
) {
    ActionBottomSheet(
        title = stringResource(R.string.receipt_attach_title),
        icon = TablerIcons.Outline.Camera,
        actions = listOf(
            SheetAction(
                text = stringResource(R.string.receipt_attach_camera),
                icon = TablerIcons.Outline.Camera,
                onClick = onCameraSelected
            ),
            SheetAction(
                text = stringResource(R.string.receipt_attach_gallery),
                icon = TablerIcons.Outline.Photo,
                onClick = onGallerySelected
            ),
            SheetAction(
                text = stringResource(R.string.receipt_attach_document),
                icon = TablerIcons.Outline.Inbox,
                onClick = onDocumentSelected
            )
        ),
        onDismiss = onDismiss
    )
}

private fun createReceiptCameraUri(context: Context): Pair<File, Uri> {
    val receiptsDir = File(context.filesDir, "receipts").also { it.mkdirs() }
    val tempFile = File.createTempFile("camera_", ".jpg", receiptsDir)
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        tempFile
    )
    return tempFile to uri
}
