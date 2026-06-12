package es.pedrazamiguez.splittrip.features.profile.presentation.component

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
import androidx.core.content.FileProvider
import java.io.File

@Composable
internal fun AvatarAttachmentHandler(
    showSheet: Boolean,
    showRemoveOption: Boolean,
    onDismissSheet: () -> Unit,
    onAvatarSelected: (uri: String, mimeType: String) -> Unit,
    onAvatarRemoved: () -> Unit
) {
    val context = LocalContext.current
    var cameraTempFile by remember { mutableStateOf<File?>(null) }
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { captured ->
        if (captured) {
            cameraImageUri?.let { uri -> onAvatarSelected(uri.toString(), "image/jpeg") }
        } else {
            cameraTempFile?.delete()
        }
        cameraTempFile = null
        cameraImageUri = null
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            val mimeType = context.contentResolver.getType(it) ?: "image/jpeg"
            onAvatarSelected(it.toString(), mimeType)
        }
    }

    if (showSheet) {
        AvatarSelectionSheet(
            showRemoveOption = showRemoveOption,
            onCameraSelected = {
                onDismissSheet()
                val (tempFile, uri) = createAvatarCameraUri(context)
                cameraTempFile = tempFile
                cameraImageUri = uri
                cameraLauncher.launch(uri)
            },
            onGallerySelected = {
                onDismissSheet()
                galleryLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onRemoveSelected = {
                onDismissSheet()
                onAvatarRemoved()
            },
            onDismiss = onDismissSheet
        )
    }
}

private fun createAvatarCameraUri(context: Context): Pair<File, Uri> {
    val avatarsDir = File(context.filesDir, "avatars_temp").also { it.mkdirs() }
    val tempFile = File.createTempFile("avatar_camera_", ".jpg", avatarsDir)
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        tempFile
    )
    return tempFile to uri
}
