package es.pedrazamiguez.splittrip.domain.service

import es.pedrazamiguez.splittrip.domain.model.report.ExportedTripReportFile
import es.pedrazamiguez.splittrip.domain.model.report.TripReportExportPayload
import es.pedrazamiguez.splittrip.domain.model.report.TripReportFormat

interface TripReportExportService {
    suspend fun export(
        payload: TripReportExportPayload,
        format: TripReportFormat
    ): ExportedTripReportFile

    suspend fun saveToUri(
        sourceContentUri: String,
        destinationUri: String
    )
}
