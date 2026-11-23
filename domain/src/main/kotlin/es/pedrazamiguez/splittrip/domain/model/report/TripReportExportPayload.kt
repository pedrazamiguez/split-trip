package es.pedrazamiguez.splittrip.domain.model.report

data class TripReportExportPayload(
    val fileBaseName: String,
    val reportTitle: String,
    val groupName: String,
    val groupDescription: String,
    val periodLabel: String,
    val generatedAtLabel: String,
    val baseCurrency: String,
    val summarySectionTitle: String,
    val summaryMetrics: List<TripReportKeyValueRow>,
    val memberBalancesSection: TripReportTableSection,
    val subunitBalancesSection: TripReportTableSection?,
    val settlementsSection: TripReportTableSection,
    val expensesSection: TripReportTableSection,
    val contributionsSection: TripReportTableSection,
    val withdrawalsSection: TripReportTableSection,
    val emptySectionLabel: String,
    val pageFooterTemplate: String,
    val csvSections: List<TripReportCsvSection>
)

data class TripReportKeyValueRow(
    val label: String,
    val value: String
)

data class TripReportTableSection(
    val title: String,
    val headers: List<String>,
    val rows: List<List<String>>
)

data class TripReportCsvSection(
    val title: String,
    val headers: List<String>,
    val rows: List<List<String>>
)

data class ExportedTripReportFile(
    val contentUri: String,
    val fileName: String,
    val mimeType: String
)
