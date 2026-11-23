package es.pedrazamiguez.splittrip.data.local.service.export

import es.pedrazamiguez.splittrip.domain.model.report.TripReportCsvSection
import es.pedrazamiguez.splittrip.domain.model.report.TripReportExportPayload

class TripReportCsvFormatter {

    fun format(payload: TripReportExportPayload): String {
        val lines = mutableListOf<String>()

        appendRow(lines, listOf(KEY_REPORT_TITLE, payload.reportTitle))
        appendRow(lines, listOf(KEY_GROUP_NAME, payload.groupName))
        if (payload.groupDescription.isNotBlank()) {
            appendRow(lines, listOf(KEY_GROUP_DESCRIPTION, payload.groupDescription))
        }
        appendRow(lines, listOf(KEY_PERIOD, payload.periodLabel))
        appendRow(lines, listOf(KEY_GENERATED_AT, payload.generatedAtLabel))
        appendRow(lines, listOf(KEY_BASE_CURRENCY, payload.baseCurrency))

        payload.csvSections.forEach { section ->
            lines.add("")
            appendSection(lines, section, payload.emptySectionLabel)
        }

        return lines.joinToString(separator = CRLF, postfix = CRLF)
    }

    internal fun escapeField(value: String): String {
        val requiresQuoting = value.contains(',') ||
            value.contains('"') ||
            value.contains('\r') ||
            value.contains('\n')
        if (!requiresQuoting) return value
        val escapedQuotes = value.replace("\"", "\"\"")
        return "\"$escapedQuotes\""
    }

    private fun appendSection(
        lines: MutableList<String>,
        section: TripReportCsvSection,
        emptySectionLabel: String
    ) {
        appendRow(lines, listOf(section.title))
        if (section.headers.isNotEmpty()) {
            appendRow(lines, section.headers)
        }
        if (section.rows.isEmpty()) {
            appendRow(lines, listOf(emptySectionLabel))
        } else {
            section.rows.forEach { row -> appendRow(lines, row) }
        }
    }

    private fun appendRow(lines: MutableList<String>, columns: List<String>) {
        lines.add(columns.joinToString(separator = ",") { escapeField(it) })
    }

    private companion object {
        const val CRLF = "\r\n"
        const val KEY_REPORT_TITLE = "ReportTitle"
        const val KEY_GROUP_NAME = "GroupName"
        const val KEY_GROUP_DESCRIPTION = "GroupDescription"
        const val KEY_PERIOD = "Period"
        const val KEY_GENERATED_AT = "GeneratedAt"
        const val KEY_BASE_CURRENCY = "BaseCurrency"
    }
}
