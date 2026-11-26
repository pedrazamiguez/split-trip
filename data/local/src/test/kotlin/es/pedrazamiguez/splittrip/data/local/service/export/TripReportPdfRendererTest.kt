package es.pedrazamiguez.splittrip.data.local.service.export

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
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
import java.util.Locale
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
        private val canvas: Canvas,
        private val pdfPayloadFactory: (Int) -> ByteArray = { "%PDF-1.4\n".toByteArray() }
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
            outputStream.write(pdfPayloadFactory(startedPages))
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

    @Test
    fun `createPaints disables OpenType ligatures on all PDF text paints`() {
        val paints = TripReportPdfTheme.createPaints(context = null)
        val textPaints = listOf(
            paints.brandSplit,
            paints.brandTrip,
            paints.brandTagline,
            paints.periodPillText,
            paints.kicker,
            paints.heroTitle,
            paints.heroSubtitle,
            paints.heroMeta,
            paints.kpiLabel,
            paints.kpiValue,
            paints.sectionTitle,
            paints.tableHeader,
            paints.tableCell,
            paints.badgeText,
            paints.upsellTitle,
            paints.upsellBody,
            paints.footerBrand,
            paints.footerPage,
            paints.footerCtaTitle,
            paints.footerCtaSubtitle,
            paints.footerCtaUrl,
            paints.qrCaption
        )

        textPaints.forEach { paint ->
            assertEquals(
                TripReportPdfTheme.DISABLED_LIGATURES_FONT_FEATURES,
                paint.fontFeatureSettings
            )
        }
    }

    @Test
    fun `createPaints enables linear subpixel unhinted text flags while keeping ligatures disabled`() {
        val paints = TripReportPdfTheme.createPaints(context = null)
        val textPaints = listOf(
            paints.brandSplit,
            paints.brandTrip,
            paints.brandTagline,
            paints.periodPillText,
            paints.kicker,
            paints.heroTitle,
            paints.heroSubtitle,
            paints.heroMeta,
            paints.kpiLabel,
            paints.kpiValue,
            paints.sectionTitle,
            paints.tableHeader,
            paints.tableCell,
            paints.badgeText,
            paints.upsellTitle,
            paints.upsellBody,
            paints.footerBrand,
            paints.footerPage,
            paints.footerCtaTitle,
            paints.footerCtaSubtitle,
            paints.footerCtaUrl,
            paints.qrCaption
        )

        assertEquals(22, textPaints.size)
        textPaints.forEach { paint ->
            assertTrue((paint.flags and Paint.ANTI_ALIAS_FLAG) != 0)
            assertTrue((paint.flags and Paint.LINEAR_TEXT_FLAG) != 0)
            assertTrue((paint.flags and Paint.SUBPIXEL_TEXT_FLAG) != 0)
            assertEquals(Paint.HINTING_OFF, paint.hinting)
            assertEquals(
                TripReportPdfTheme.DISABLED_LIGATURES_FONT_FEATURES,
                paint.fontFeatureSettings
            )
        }
    }

    @Test
    fun `render embeds URI link annotations for running footer and brand footer card`() {
        val bitmap = Bitmap.createBitmap(595, 842, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val multiPageSession = FakePdfDocumentSession(
            canvas = canvas,
            pdfPayloadFactory = ::buildMinimalMultiPagePdf
        )
        val multiPageRenderer = TripReportPdfRenderer(context = null, sessionFactory = { multiPageSession })
        val outputFile = File(tempDir, "summary-links.pdf")
        val payload = createFullPayload(expenseRowsCount = 55)

        multiPageRenderer.render(payload, outputFile)

        val resultText = outputFile.readBytes().toString(Charsets.ISO_8859_1)
        assertTrue(resultText.contains("/Prev "))
        assertTrue(resultText.contains("startxref"))
        val pageCount = multiPageSession.startedPages
        assertTrue(pageCount >= 2)

        val linkAction = "/Subtype /Link"
        val uriAction = "/A << /Type /Action /S /URI /URI (https://splittrip.eu) >>"
        val borderEntry = "/Border [0 0 0]"
        val totalLinks = Regex(Regex.escape(linkAction)).findAll(resultText).count()
        // Every page has running footer (pageCount links) + final page has brand footer card (+1 link)
        assertEquals(pageCount + 1, totalLinks)
        assertEquals(pageCount + 1, Regex(Regex.escape(uriAction)).findAll(resultText).count())
        assertEquals(pageCount + 1, Regex(Regex.escape(borderEntry)).findAll(resultText).count())

        val rectMatches = Regex("""/Rect \[(\d+\.\d+) (\d+\.\d+) (\d+\.\d+) (\d+\.\d+)]""").findAll(resultText).toList()
        assertEquals(pageCount + 1, rectMatches.size)
        rectMatches.forEach { match ->
            val llx = match.groupValues[1].toFloat()
            val lly = match.groupValues[2].toFloat()
            val urx = match.groupValues[3].toFloat()
            val ury = match.groupValues[4].toFloat()
            assertTrue(llx in 0f..595f)
            assertTrue(urx in 0f..595f)
            assertTrue(llx < urx)
            assertTrue(lly in 0f..842f)
            assertTrue(ury in 0f..842f)
            assertTrue(lly < ury)
        }
    }

    @Test
    fun `drawBrandFooterCard and drawRunningPageFooter record link bounds anchored to splittrip eu text`() {
        val bitmap = Bitmap.createBitmap(595, 842, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paints = TripReportPdfTheme.createPaints(context = null)
        val brandFooter = createSampleBrandFooter()

        val cardLinks = mutableListOf<PdfLinkAnnotation>()
        TripReportPdfBrandDrawers.drawBrandFooterCard(
            canvas = canvas,
            brandFooter = brandFooter,
            paints = paints,
            topY = 600f,
            links = cardLinks
        )
        assertEquals(1, cardLinks.size)
        assertEquals("https://splittrip.eu", cardLinks.single().uri)
        val expectedCardLeftX = TripReportPdfMetrics.MARGIN_X_PT + 16f
        assertTrue(cardLinks.single().rectCanvasPt.left < expectedCardLeftX)
        assertTrue(cardLinks.single().rectCanvasPt.right > expectedCardLeftX)

        val runningLinks = mutableListOf<PdfLinkAnnotation>()
        TripReportPdfBrandDrawers.drawRunningPageFooter(
            canvas = canvas,
            websiteDisplay = brandFooter.websiteDisplay,
            websiteUrl = brandFooter.websiteUrl,
            pageFooterText = "Page 1 of 2",
            paints = paints,
            links = runningLinks
        )
        assertEquals(1, runningLinks.size)
        assertEquals("https://splittrip.eu", runningLinks.single().uri)
        val prefixWidth = paints.footerBrand.measureText("SplitTrip  ·  ")
        assertTrue(runningLinks.single().rectCanvasPt.left >= TripReportPdfMetrics.MARGIN_X_PT + prefixWidth - 3f)
        assertTrue(runningLinks.single().rectCanvasPt.right > runningLinks.single().rectCanvasPt.left)
    }

    private fun buildMinimalMultiPagePdf(pageCount: Int): ByteArray {
        val sb = StringBuilder("%PDF-1.4\n")
        val offsets = mutableMapOf<Int, Int>()
        // 1: Catalog, 2: Pages, 3..2+pageCount: Page objects
        val kidsRefs = (0 until pageCount).joinToString(" ") { "${3 + it} 0 R" }

        offsets[1] = sb.length
        sb.append("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n")

        offsets[2] = sb.length
        sb.append("2 0 obj\n<< /Type /Pages /Count $pageCount /Kids [$kidsRefs] >>\nendobj\n")

        for (index in 0 until pageCount) {
            val objId = 3 + index
            offsets[objId] = sb.length
            sb.append("$objId 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] >>\nendobj\n")
        }

        val totalObjects = 3 + pageCount
        val xrefOffset = sb.length
        sb.append("xref\n0 $totalObjects\n")
        sb.append("0000000000 65535 f \n")
        for (objId in 1 until totalObjects) {
            sb.append(String.format(Locale.US, "%010d 00000 n \n", offsets.getValue(objId)))
        }
        sb.append("trailer\n<< /Size $totalObjects /Root 1 0 R >>\n")
        sb.append("startxref\n$xrefOffset\n%%EOF\n")
        return sb.toString().toByteArray(Charsets.ISO_8859_1)
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
