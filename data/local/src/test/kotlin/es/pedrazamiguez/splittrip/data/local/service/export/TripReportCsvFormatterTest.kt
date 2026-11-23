package es.pedrazamiguez.splittrip.data.local.service.export

import es.pedrazamiguez.splittrip.domain.model.report.TripReportCsvSection
import es.pedrazamiguez.splittrip.domain.model.report.TripReportExportPayload
import es.pedrazamiguez.splittrip.domain.model.report.TripReportTableSection
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TripReportCsvFormatterTest {

    private val formatter = TripReportCsvFormatter()

    @Test
    fun `format serializes sections with CRLF line endings and preserves BigDecimal plain strings`() {
        val emptySection = TripReportTableSection("Empty", emptyList(), emptyList())
        val payload = TripReportExportPayload(
            fileBaseName = "splittrip-statement-flat-2026-11-20",
            reportTitle = "Group Financial Statement",
            groupName = "Shared Flat",
            groupDescription = "Monthly utilities",
            periodLabel = "Full ledger",
            generatedAtLabel = "Generated 20 Nov 2026",
            baseCurrency = "EUR",
            summarySectionTitle = "Financial Summary",
            summaryMetrics = emptyList(),
            memberBalancesSection = emptySection,
            subunitBalancesSection = null,
            settlementsSection = emptySection,
            expensesSection = emptySection,
            contributionsSection = emptySection,
            withdrawalsSection = emptySection,
            emptySectionLabel = "No entries",
            pageFooterTemplate = "Page %1\$d of %2\$d",
            csvSections = listOf(
                TripReportCsvSection(
                    title = "Itemized Expenses",
                    headers = listOf("Title", "Amount", "Rate"),
                    rows = listOf(
                        listOf("Electricity", "125.50", "0.027193")
                    )
                )
            )
        )

        val csv = formatter.format(payload)

        assertTrue(csv.endsWith("\r\n"))
        assertTrue(csv.contains("ReportTitle,Group Financial Statement\r\n"))
        assertTrue(csv.contains("GroupName,Shared Flat\r\n"))
        assertTrue(csv.contains("BaseCurrency,EUR\r\n"))
        assertTrue(csv.contains("\r\nItemized Expenses\r\nTitle,Amount,Rate\r\nElectricity,125.50,0.027193\r\n"))
    }

    @Test
    fun `escapeField quotes fields containing commas double quotes and newlines per RFC 4180`() {
        assertEquals("PlainText", formatter.escapeField("PlainText"))
        assertEquals("\"Dinner, drinks\"", formatter.escapeField("Dinner, drinks"))
        assertEquals("\"Cafe \"\"Central\"\"\"", formatter.escapeField("Cafe \"Central\""))
        assertEquals("\"Line 1\nLine 2\"", formatter.escapeField("Line 1\nLine 2"))
        assertEquals("\"Line 1\r\nLine 2\"", formatter.escapeField("Line 1\r\nLine 2"))
    }
}
