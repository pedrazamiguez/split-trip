package es.pedrazamiguez.splittrip.data.local.service.export

import android.graphics.Bitmap
import android.graphics.Canvas
import es.pedrazamiguez.splittrip.domain.model.report.TripReportBrandFooter
import es.pedrazamiguez.splittrip.domain.model.report.TripReportCell
import es.pedrazamiguez.splittrip.domain.model.report.TripReportCellAlign
import es.pedrazamiguez.splittrip.domain.model.report.TripReportExportPayload
import es.pedrazamiguez.splittrip.domain.model.report.TripReportMetricCard
import es.pedrazamiguez.splittrip.domain.model.report.TripReportProUpsellCallout
import es.pedrazamiguez.splittrip.domain.model.report.TripReportTableSection
import es.pedrazamiguez.splittrip.domain.model.report.TripReportTone
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
        renderer = TripReportPdfRenderer(context = null, sessionFactory = { fakeSession })
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun `render writes valid multi-page Pro PDF with all section types and QR brand footer`() {
        val outputFile = File(tempDir, "summary-pro.pdf")
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
    fun `render writes Free summary PDF with Pro upsell callout and no itemised sections`() {
        val outputFile = File(tempDir, "summary-free.pdf")
        val payload = createFullPayload(expenseRowsCount = 0).copy(
            expensesSection = null,
            contributionsSection = null,
            withdrawalsSection = null,
            proUpsellCallout = TripReportProUpsellCallout(
                badge = "PRO",
                title = "Itemised breakdown reserved for SplitTrip Pro",
                body = "Upgrade to SplitTrip Pro for unlimited itemised PDFs and CSV spreadsheets."
            )
        )

        val pages = renderer.buildPagePlans(payload)
        assertTrue(pages.flatten().any { it is PdfBlock.ProUpsellCard })
        assertTrue(pages.flatten().any { it is PdfBlock.BrandFooterCard })

        renderer.render(payload, outputFile)

        assertTrue(outputFile.exists())
        assertTrue(outputFile.length() > 0L)
        assertEquals(fakeSession.startedPages, fakeSession.finishedPages)
        assertTrue(fakeSession.closed)
    }

    @Test
    fun `resolveColumnWidths distributes table width proportionally without overflow`() {
        val weights = listOf(0.22f, 0.16f, 0.16f, 0.16f, 0.14f, 0.16f)
        val widths = TripReportPdfSectionDrawers.resolveColumnWidths(6, weights)

        assertEquals(6, widths.size)
        assertEquals(TripReportPdfMetrics.TABLE_INNER_WIDTH_PT, widths.sum(), 0.01f)
        assertTrue(widths.all { it >= 60f })
    }

    private fun createFullPayload(expenseRowsCount: Int): TripReportExportPayload {
        val longTitle = "Very long restaurant and entertainment description exceeding single column width"
        val expenseRows = (1..expenseRowsCount).map { index ->
            listOf(
                TripReportCell("20 Nov", TripReportTone.MUTED),
                TripReportCell("$longTitle #$index"),
                TripReportCell("Food", TripReportTone.MUTED),
                TripReportCell("Alice"),
                TripReportCell("€12.50", TripReportTone.MUTED, TripReportCellAlign.END),
                TripReportCell("€14.00", TripReportTone.NEGATIVE, TripReportCellAlign.END),
                TripReportCell("Finished", TripReportTone.BADGE_SUCCESS, TripReportCellAlign.END)
            )
        }
        return TripReportExportPayload(
            fileBaseName = "splittrip-summary-flat-2026-11-20",
            reportTitle = "Group summary & itemised ledger",
            groupName = "Shared Flat & Household Budget",
            groupDescription = "Monthly recurring household ledger",
            periodLabel = "1 Nov – 20 Nov",
            generatedAtLabel = "Generated 20 Nov 2026",
            baseCurrency = "EUR",
            summarySectionTitle = "Group overview",
            summaryMetrics = createSampleMetrics(),
            memberBalancesSection = createSampleBalanceSection("Member balances", "Member", "Alice"),
            subunitBalancesSection = createSampleBalanceSection("Subunit balances", "Subunit", "Couple"),
            settlementsSection = createSampleSettlementsSection(),
            expensesSection = TripReportTableSection(
                title = "Itemised expenses",
                accentTone = TripReportTone.NEGATIVE,
                headers = listOf("Date", "Title", "Category", "Paid by", "Source", "Effective", "Status"),
                rows = expenseRows
            ),
            contributionsSection = TripReportTableSection(
                title = "Contributions",
                accentTone = TripReportTone.POSITIVE,
                headers = emptyList(),
                rows = emptyList()
            ),
            withdrawalsSection = createSampleWithdrawalsSection(),
            proUpsellCallout = null,
            brandFooter = createSampleBrandFooter(),
            emptySectionLabel = "No activity recorded in this period",
            pageFooterTemplate = "Page %1\$d of %2\$d",
            csvSections = emptyList()
        )
    }

    private fun createSampleMetrics(): List<TripReportMetricCard> = listOf(
        TripReportMetricCard("Virtual pocket", "€380.00", TripReportTone.PRIMARY),
        TripReportMetricCard("Total contributed", "€500.00", TripReportTone.POSITIVE),
        TripReportMetricCard("Total expenses", "€120.00", TripReportTone.NEGATIVE),
        TripReportMetricCard("Cash in hand", "€40.00", TripReportTone.TEAL),
        TripReportMetricCard("Extras & fees", "€4.50", TripReportTone.AMBER)
    )

    private fun createSampleBalanceSection(
        title: String,
        entityHeader: String,
        entityName: String
    ): TripReportTableSection = TripReportTableSection(
        title = title,
        accentTone = TripReportTone.PRIMARY,
        columnWeights = listOf(0.22f, 0.16f, 0.16f, 0.16f, 0.14f, 0.16f),
        columnAlignments = listOf(
            TripReportCellAlign.START,
            TripReportCellAlign.END,
            TripReportCellAlign.END,
            TripReportCellAlign.END,
            TripReportCellAlign.END,
            TripReportCellAlign.END
        ),
        headers = listOf(entityHeader, "Contributed", "Spent", "Pocket", "Cash", "Net"),
        rows = listOf(
            listOf(
                TripReportCell(entityName),
                TripReportCell("€500.00", TripReportTone.POSITIVE, TripReportCellAlign.END),
                TripReportCell("€120.00", TripReportTone.NEGATIVE, TripReportCellAlign.END),
                TripReportCell("€380.00", TripReportTone.POSITIVE, TripReportCellAlign.END),
                TripReportCell("€0.00", TripReportTone.TEAL, TripReportCellAlign.END),
                TripReportCell("€380.00", TripReportTone.POSITIVE, TripReportCellAlign.END)
            )
        )
    )

    private fun createSampleSettlementsSection(): TripReportTableSection = TripReportTableSection(
        title = "Settlements",
        accentTone = TripReportTone.TEAL,
        headers = listOf("From", "To", "Source", "Amount", "Status"),
        rows = listOf(
            listOf(
                TripReportCell("Bob"),
                TripReportCell("Alice"),
                TripReportCell("Net", TripReportTone.MUTED),
                TripReportCell("€50.00", TripReportTone.PRIMARY, TripReportCellAlign.END),
                TripReportCell("Pending", TripReportTone.BADGE_WARNING, TripReportCellAlign.END)
            )
        )
    )

    private fun createSampleWithdrawalsSection(): TripReportTableSection = TripReportTableSection(
        title = "Cash withdrawals",
        accentTone = TripReportTone.AMBER,
        headers = listOf("Date", "Member", "Amount", "Remaining", "Deducted"),
        rows = listOf(
            listOf(
                TripReportCell("2 Nov", TripReportTone.MUTED),
                TripReportCell("Alice"),
                TripReportCell("€100.00", TripReportTone.AMBER, TripReportCellAlign.END),
                TripReportCell("€40.00", TripReportTone.TEAL, TripReportCellAlign.END),
                TripReportCell("€102.00", TripReportTone.DEFAULT, TripReportCellAlign.END)
            )
        )
    )

    private fun createSampleBrandFooter(): TripReportBrandFooter = TripReportBrandFooter(
        brandTagline = "Split expenses without borders",
        ctaHeadline = "Share expenses fairly. Zero stress.",
        ctaSubtitle = "Offline-first multi-currency group pockets on Android.",
        websiteDisplay = "splittrip.eu",
        qrUrl = "https://splittrip.eu",
        qrCaption = "Scan to get SplitTrip"
    )
}
