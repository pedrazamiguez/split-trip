package es.pedrazamiguez.splittrip.core.designsystem.presentation.model

import android.net.Uri
import java.io.File

data class CameraAttachmentTarget(
    val file: File,
    val uri: Uri
)
