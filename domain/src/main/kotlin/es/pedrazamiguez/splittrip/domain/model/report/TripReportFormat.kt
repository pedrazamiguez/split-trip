package es.pedrazamiguez.splittrip.domain.model.report

enum class TripReportFormat(
    val fileExtension: String,
    val mimeType: String
) {
    PDF(fileExtension = "pdf", mimeType = "application/pdf"),
    CSV(fileExtension = "csv", mimeType = "text/csv")
}
