package es.pedrazamiguez.splittrip.features.balance.presentation.util

import android.content.ActivityNotFoundException
import androidx.activity.result.ActivityResultLauncher
import timber.log.Timber

internal object ReportSaveLauncher {

    fun launchCreateDocument(
        launcher: ActivityResultLauncher<CreateTripReportDocumentRequest>,
        request: CreateTripReportDocumentRequest
    ): Boolean = try {
        launcher.launch(request)
        true
    } catch (e: ActivityNotFoundException) {
        Timber.w(e, "No file picker found to save exported report (${request.mimeType})")
        false
    } catch (e: SecurityException) {
        Timber.e(e, "Security error launching file picker for report (${request.mimeType})")
        false
    }
}
