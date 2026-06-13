package es.pedrazamiguez.splittrip.core.designsystem.permission

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

@Composable
fun rememberRequestCameraPermission(onPermissionResult: (Boolean) -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = onPermissionResult
    )
    return remember(launcher, context) {
        {
            val currentStatus = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
            if (currentStatus == PackageManager.PERMISSION_GRANTED) {
                onPermissionResult(true)
            } else {
                launcher.launch(Manifest.permission.CAMERA)
            }
        }
    }
}

fun checkCameraPermission(context: android.content.Context): Boolean {
    return ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
}
