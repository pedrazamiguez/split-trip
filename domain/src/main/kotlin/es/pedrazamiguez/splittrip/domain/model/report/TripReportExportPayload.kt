package es.pedrazamiguez.splittrip.domain.model.report

enum class TripReportTone {
    DEFAULT,
    MUTED,
    PRIMARY,
    POSITIVE,
    NEGATIVE,
    TEAL,
    AMBER,
    BADGE_SUCCESS,
    BADGE_WARNING,
    BADGE_ERROR,
    BADGE_INFO
}

enum class TripReportCellAlign {
    START,
    END
}

data class TripReportCell(
    val text: String,
    val tone: TripReportTone = TripReportTone.DEFAULT,
    val align: TripReportCellAlign = TripReportCellAlign.START
)

data class TripReportMetricCard(
    val label: String,
    val value: String,
    val tone: TripReportTone = TripReportTone.DEFAULT
)

data class TripReportProUpsellCallout(
    val badge: String,
    val title: String,
    val body: String
)

data class TripReportBrandFooter(
    val brandTagline: String,
    val ctaHeadline: String,
    val ctaSubtitle: String,
    val websiteDisplay: String,
    val qrUrl: String,
    val qrCaption: String
)

data class TripReportExportPayload(
    val fileBaseName: String,
    val reportTitle: String,
    val groupName: String,
    val groupDescription: String,
    val periodLabel: String,
    val generatedAtLabel: String,
    val baseCurrency: String,
    val summarySectionTitle: String,
    val summaryMetrics: List<TripReportMetricCard>,
    val memberBalancesSection: TripReportTableSection,
    val subunitBalancesSection: TripReportTableSection?,
    val settlementsSection: TripReportTableSection,
    val expensesSection: TripReportTableSection?,
    val contributionsSection: TripReportTableSection?,
    val withdrawalsSection: TripReportTableSection?,
    val proUpsellCallout: TripReportProUpsellCallout?,
    val brandFooter: TripReportBrandFooter,
    val emptySectionLabel: String,
    val pageFooterTemplate: String,
    val csvSections: List<TripReportCsvSection>
)

data class TripReportTableSection(
    val title: String,
    val accentTone: TripReportTone = TripReportTone.PRIMARY,
    val columnWeights: List<Float> = emptyList(),
    val columnAlignments: List<TripReportCellAlign> = emptyList(),
    val headers: List<String>,
    val rows: List<List<TripReportCell>>
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
