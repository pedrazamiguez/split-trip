package es.pedrazamiguez.splittrip.features.balance.presentation.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract

internal data class CreateTripReportDocumentRequest(
    val fileName: String,
    val mimeType: String
)

internal class CreateTripReportDocumentContract :
    ActivityResultContract<CreateTripReportDocumentRequest, Uri?>() {

    override fun createIntent(
        context: Context,
        input: CreateTripReportDocumentRequest
    ): Intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
        addCategory(Intent.CATEGORY_OPENABLE)
        type = input.mimeType
        putExtra(Intent.EXTRA_TITLE, input.fileName)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? =
        intent?.takeIf { resultCode == Activity.RESULT_OK }?.data
}
