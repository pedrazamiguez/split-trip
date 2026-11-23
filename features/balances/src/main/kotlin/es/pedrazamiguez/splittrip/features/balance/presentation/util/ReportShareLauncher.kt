package es.pedrazamiguez.splittrip.features.balance.presentation.util

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import timber.log.Timber

internal object ReportShareLauncher {

    fun launchShareChooser(
        context: Context,
        contentUri: String,
        mimeType: String,
        chooserTitle: String
    ): Boolean = try {
        val uri = Uri.parse(contentUri)
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newUri(context.contentResolver, chooserTitle, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooserIntent = Intent.createChooser(sendIntent, chooserTitle).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooserIntent)
        true
    } catch (e: ActivityNotFoundException) {
        Timber.w(e, "No activity found to share exported report ($mimeType)")
        false
    } catch (e: SecurityException) {
        Timber.e(e, "Security error sharing exported report ($mimeType)")
        false
    }
}
