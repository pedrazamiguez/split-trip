package es.pedrazamiguez.splittrip.data.local.service

import android.content.Context
import androidx.core.content.FileProvider
import es.pedrazamiguez.splittrip.data.local.service.export.TripReportCsvFormatter
import es.pedrazamiguez.splittrip.data.local.service.export.TripReportPdfRenderer
import es.pedrazamiguez.splittrip.domain.model.report.ExportedTripReportFile
import es.pedrazamiguez.splittrip.domain.model.report.TripReportExportPayload
import es.pedrazamiguez.splittrip.domain.model.report.TripReportFormat
import es.pedrazamiguez.splittrip.domain.service.TripReportExportService
import java.io.File
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TripReportExportServiceImpl(
    private val context: Context,
    private val csvFormatter: TripReportCsvFormatter = TripReportCsvFormatter(),
    private val pdfRenderer: TripReportPdfRenderer = TripReportPdfRenderer(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val uriResolver: (Context, File) -> String = { ctx, file ->
        FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file).toString()
    }
) : TripReportExportService {

    override suspend fun export(
        payload: TripReportExportPayload,
        format: TripReportFormat
    ): ExportedTripReportFile = withContext(ioDispatcher) {
        val exportsDir = File(context.cacheDir, EXPORTS_DIR_NAME).apply { mkdirs() }
        purgeStaleExports(exportsDir)

        val sanitizedBaseName = sanitizeFileBaseName(payload.fileBaseName)
        val outputFile = File(exportsDir, "$sanitizedBaseName.${format.fileExtension}")

        when (format) {
            TripReportFormat.CSV -> {
                val csvContent = csvFormatter.format(payload)
                outputFile.writeText(csvContent, Charsets.UTF_8)
            }
            TripReportFormat.PDF -> {
                pdfRenderer.render(payload, outputFile)
            }
        }

        val contentUri = uriResolver(context, outputFile)
        ExportedTripReportFile(
            contentUri = contentUri,
            fileName = outputFile.name,
            mimeType = format.mimeType
        )
    }

    private fun purgeStaleExports(exportsDir: File) {
        val cutoffMillis = System.currentTimeMillis() - STALE_EXPORT_MAX_AGE_MS
        exportsDir.listFiles()
            ?.filter { it.isFile && it.lastModified() < cutoffMillis }
            ?.forEach { it.delete() }
    }

    private fun sanitizeFileBaseName(rawName: String): String =
        rawName
            .replace(INVALID_FILENAME_CHARS_REGEX, "_")
            .trim('_')
            .ifBlank { DEFAULT_FILE_BASE_NAME }

    private companion object {
        const val EXPORTS_DIR_NAME = "exports"
        const val DEFAULT_FILE_BASE_NAME = "splittrip-statement"
        const val STALE_EXPORT_MAX_AGE_MS = 24L * 60L * 60L * 1000L
        val INVALID_FILENAME_CHARS_REGEX = Regex("[^a-zA-Z0-9._-]")
    }
}
