package es.pedrazamiguez.splittrip.features.group.presentation.component

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.FileProvider
import es.pedrazamiguez.splittrip.core.designsystem.R as DesignSystemR
import es.pedrazamiguez.splittrip.core.designsystem.permission.rememberRequestCameraPermission
import es.pedrazamiguez.splittrip.core.designsystem.presentation.notification.LocalTopPillController
import java.io.File

@Composable
internal fun GroupImageAttachmentHandler(
    showSheet: Boolean,
    showRemoveOption: Boolean,
    onDismissSheet: () -> Unit,
    onImageSelected: (uri: String) -> Unit,
    onImageRemoved: () -> Unit
) {
    val context = LocalContext.current
    val pillController = LocalTopPillController.current
    val cameraPermissionRequiredMessage = stringResource(DesignSystemR.string.camera_permission_required)

    var cameraTempFilePath by rememberSaveable { mutableStateOf<String?>(null) }
    var cameraImageUriStr by rememberSaveable { mutableStateOf<String?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { captured ->
        if (captured) {
            cameraImageUriStr?.let { uriStr -> onImageSelected(uriStr) }
        } else {
            cameraTempFilePath?.let { path -> File(path).delete() }
        }
        cameraTempFilePath = null
        cameraImageUriStr = null
    }

    val requestCameraPermission = rememberRequestCameraPermission { isGranted ->
        if (isGranted) {
            val (tempFile, uri) = createGroupCameraUri(context)
            cameraTempFilePath = tempFile.absolutePath
            cameraImageUriStr = uri.toString()
            cameraLauncher.launch(uri)
        } else {
            pillController.showPill(cameraPermissionRequiredMessage)
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            onImageSelected(it.toString())
        }
    }

    GroupImageAttachmentSheet(
        showSheet = showSheet,
        showRemoveOption = showRemoveOption,
        onDismissSheet = onDismissSheet,
        onCameraClick = {
            onDismissSheet()
            requestCameraPermission()
        },
        onGalleryClick = {
            onDismissSheet()
            galleryLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        },
        onImageRemoved = {
            onDismissSheet()
            onImageRemoved()
        }
    )
}

private fun createGroupCameraUri(context: Context): Pair<File, Uri> {
    val groupsTempDir = File(context.filesDir, "groups_temp").also { it.mkdirs() }
    val tempFile = File.createTempFile("group_camera_", ".jpg", groupsTempDir)
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        tempFile
    )
    return tempFile to uri
}
