package es.pedrazamiguez.splittrip.data.local.service.export

import android.graphics.Bitmap
import android.graphics.Canvas
import es.pedrazamiguez.splittrip.domain.model.report.TripReportExportPayload
import es.pedrazamiguez.splittrip.domain.model.report.TripReportKeyValueRow
import es.pedrazamiguez.splittrip.domain.model.report.TripReportTableSection
import java.io.File
import java.io.OutputStream
import java.nio.file.Files
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TripReportPdfRendererTest {

    private class FakePdfDocumentSession(
        private val canvas: Canvas
    ) : PdfDocumentSession {
        var startedPages: Int = 0
        var finishedPages: Int = 0
        var closed: Boolean = false

        override fun startPage(width: Int, height: Int, pageNumber: Int): Canvas {
            startedPages++
            return canvas
        }

        override fun finishCurrentPage() {
            finishedPages++
        }

        override fun writeTo(outputStream: OutputStream) {
            outputStream.write("%PDF-1.4\n".toByteArray())
        }

        override fun close() {
            closed = true
        }
    }

    private lateinit var tempDir: File
    private lateinit var fakeSession: FakePdfDocumentSession
    private lateinit var renderer: TripReportPdfRenderer

    @Before
    fun setUp() {
        tempDir = Files.createTempDirectory("pdf-renderer-test").toFile()
        val bitmap = Bitmap.createBitmap(595, 842, Bitmap.Config.ARGB_8888)
        fakeSession = FakePdfDocumentSession(Canvas(bitmap))
        renderer = TripReportPdfRenderer(sessionFactory = { fakeSession })
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun `render writes valid multi-page PDF with all section types and truncated text`() {
        val outputFile = File(tempDir, "statement.pdf")
        val payload = createFullPayload(expenseRowsCount = 55)

        renderer.render(payload, outputFile)

        assertTrue(outputFile.exists())
        assertTrue(outputFile.length() > 0L)
        val header = outputFile.readBytes().take(4).toByteArray().decodeToString()
        assertEquals("%PDF", header)
        assertTrue(fakeSession.startedPages >= 2)
        assertEquals(fakeSession.startedPages, fakeSession.finishedPages)
        assertTrue(fakeSession.closed)
    }

    @Test
    fun `render writes PDF cleanly when optional sections are empty or null`() {
        val outputFile = File(tempDir, "empty-statement.pdf")
        val emptyTable = TripReportTableSection("Section", listOf("Col"), emptyList())
        val payload = TripReportExportPayload(
            fileBaseName = "empty",
            reportTitle = "Group Financial Statement",
            groupName = "Solo Group",
            groupDescription = "",
            periodLabel = "Full ledger",
            generatedAtLabel = "Generated 20 Nov 2026",
            baseCurrency = "EUR",
            summarySectionTitle = "Summary",
            summaryMetrics = listOf(TripReportKeyValueRow("Total", "€0.00")),
            memberBalancesSection = emptyTable,
            subunitBalancesSection = null,
            settlementsSection = emptyTable,
            expensesSection = emptyTable,
            contributionsSection = emptyTable,
            withdrawalsSection = emptyTable,
            emptySectionLabel = "No entries in this period",
            pageFooterTemplate = "invalid-%z",
            csvSections = emptyList()
        )

        renderer.render(payload, outputFile)

        assertTrue(outputFile.exists())
        assertTrue(outputFile.length() > 0L)
        assertEquals(1, fakeSession.startedPages)
        assertEquals(1, fakeSession.finishedPages)
        assertTrue(fakeSession.closed)
    }

    private fun createFullPayload(expenseRowsCount: Int): TripReportExportPayload {
        val longTitle = "Very long restaurant and entertainment description exceeding single column width"
        val expenseRows = (1..expenseRowsCount).map { index ->
            listOf("20 Nov", "$longTitle #$index", "Food", "Alice", "€12.50", "€14.00", "Finished")
        }
        return TripReportExportPayload(
            fileBaseName = "splittrip-statement-flat-2026-11-20",
            reportTitle = "Group Financial Statement",
            groupName = "Shared Flat & Household Budget",
            groupDescription = "Monthly recurring household ledger",
            periodLabel = "1 Nov - 20 Nov",
            generatedAtLabel = "Generated 20 Nov 2026",
            baseCurrency = "EUR",
            summarySectionTitle = "Financial Summary",
            summaryMetrics = listOf(
                TripReportKeyValueRow("Base currency", "EUR"),
                TripReportKeyValueRow("Total contributed", "€500.00")
            ),
            memberBalancesSection = TripReportTableSection(
                title = "Member Balances",
                headers = listOf("Member", "Contributed", "Spent", "Pocket", "Cash", "Net"),
                rows = listOf(listOf("Alice", "€500.00", "€120.00", "€380.00", "€0.00", "€380.00"))
            ),
            subunitBalancesSection = TripReportTableSection(
                title = "Subunit Balances",
                headers = listOf("Subunit", "Contributed", "Spent", "Pocket", "Cash", "Net"),
                rows = listOf(listOf("Couple", "€500.00", "€120.00", "€380.00", "€0.00", "€380.00"))
            ),
            settlementsSection = TripReportTableSection(
                title = "Settlement Consensus",
                headers = listOf("From", "To", "Source", "Amount", "Status"),
                rows = listOf(listOf("Bob", "Alice", "Net", "€50.00", "Pending"))
            ),
            expensesSection = TripReportTableSection(
                title = "Itemized Expenses",
                headers = listOf("Date", "Title", "Category", "Paid by", "Source", "Effective", "Status"),
                rows = expenseRows
            ),
            contributionsSection = TripReportTableSection(
                title = "Contributions",
                headers = emptyList(),
                rows = emptyList()
            ),
            withdrawalsSection = TripReportTableSection(
                title = "Cash Withdrawals",
                headers = listOf("Date", "Member", "Amount", "Remaining", "Deducted"),
                rows = listOf(listOf("2 Nov", "Alice", "€100.00", "€40.00", "€102.00"))
            ),
            emptySectionLabel = "No entries in this period",
            pageFooterTemplate = "Page %1\$d of %2\$d",
            csvSections = emptyList()
        )
    }
}
