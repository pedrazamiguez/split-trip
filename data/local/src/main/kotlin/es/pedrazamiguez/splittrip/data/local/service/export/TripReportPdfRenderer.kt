package es.pedrazamiguez.splittrip.data.local.service.export

import android.content.Context
import android.graphics.Canvas
import android.graphics.pdf.PdfDocument
import es.pedrazamiguez.splittrip.domain.model.report.TripReportBrandFooter
import es.pedrazamiguez.splittrip.domain.model.report.TripReportCell
import es.pedrazamiguez.splittrip.domain.model.report.TripReportCellAlign
import es.pedrazamiguez.splittrip.domain.model.report.TripReportExportPayload
import es.pedrazamiguez.splittrip.domain.model.report.TripReportMetricCard
import es.pedrazamiguez.splittrip.domain.model.report.TripReportProUpsellCallout
import es.pedrazamiguez.splittrip.domain.model.report.TripReportTableSection
import es.pedrazamiguez.splittrip.domain.model.report.TripReportTone
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

internal interface PdfDocumentSession {
    fun startPage(width: Int, height: Int, pageNumber: Int): Canvas
    fun finishCurrentPage()
    fun writeTo(outputStream: OutputStream)
    fun close()
}

private class AndroidPdfDocumentSession : PdfDocumentSession {
    private val document = PdfDocument()
    private var currentPage: PdfDocument.Page? = null

    override fun startPage(width: Int, height: Int, pageNumber: Int): Canvas {
        val pageInfo = PdfDocument.PageInfo.Builder(width, height, pageNumber).create()
        val page = document.startPage(pageInfo)
        currentPage = page
        return page.canvas
    }

    override fun finishCurrentPage() {
        currentPage?.let(document::finishPage)
        currentPage = null
    }

    override fun writeTo(outputStream: OutputStream) {
        document.writeTo(outputStream)
    }

    override fun close() {
        document.close()
    }
}

internal sealed interface PdfBlock {
    val heightPt: Float

    data class BrandHeader(
        val brandTagline: String,
        val periodLabel: String
    ) : PdfBlock {
        override val heightPt: Float = TripReportPdfMetrics.BRAND_HEADER_HEIGHT_PT
    }

    data class HeroGroupCard(
        val reportTitle: String,
        val groupName: String,
        val groupDescription: String,
        val metaLine: String,
        override val heightPt: Float
    ) : PdfBlock

    data class SummaryMetricsGrid(
        val metrics: List<TripReportMetricCard>,
        override val heightPt: Float
    ) : PdfBlock

    data class SectionHeading(
        val title: String,
        val accentTone: TripReportTone
    ) : PdfBlock {
        override val heightPt: Float = TripReportPdfMetrics.SECTION_HEADING_HEIGHT_PT
    }

    data class TableHeader(
        val headers: List<String>,
        val columnWeights: List<Float>,
        val columnAlignments: List<TripReportCellAlign>
    ) : PdfBlock {
        override val heightPt: Float = TripReportPdfMetrics.TABLE_HEADER_HEIGHT_PT
    }

    data class TableRow(
        val cells: List<TripReportCell>,
        val columnWeights: List<Float>,
        val columnAlignments: List<TripReportCellAlign>,
        val rowIndex: Int
    ) : PdfBlock {
        override val heightPt: Float = TripReportPdfMetrics.TABLE_ROW_HEIGHT_PT
    }

    data class EmptySection(val text: String) : PdfBlock {
        override val heightPt: Float = TripReportPdfMetrics.EMPTY_SECTION_HEIGHT_PT
    }

    data class ProUpsellCard(val callout: TripReportProUpsellCallout) : PdfBlock {
        override val heightPt: Float = TripReportPdfMetrics.PRO_UPSELL_CARD_HEIGHT_PT + 10f
    }

    data class BrandFooterCard(val brandFooter: TripReportBrandFooter) : PdfBlock {
        override val heightPt: Float = TripReportPdfMetrics.BRAND_FOOTER_CARD_HEIGHT_PT
    }

    data object Spacer : PdfBlock {
        override val heightPt: Float = TripReportPdfMetrics.SPACER_HEIGHT_PT
    }
}

class TripReportPdfRenderer internal constructor(
    private val context: Context?,
    private val sessionFactory: () -> PdfDocumentSession
) {

    constructor(context: Context? = null) : this(
        context = context,
        sessionFactory = { AndroidPdfDocumentSession() }
    )

    fun render(payload: TripReportExportPayload, outputFile: File) {
        val paints = TripReportPdfTheme.createPaints(context)
        val pagePlans = buildPagePlans(payload, paints)
        val totalPages = pagePlans.size.coerceAtLeast(1)
        val session = sessionFactory()

        try {
            val linksByPage = mutableMapOf<Int, List<PdfLinkAnnotation>>()
            pagePlans.forEachIndexed { index, blocks ->
                val pageNumber = index + 1
                val canvas = session.startPage(
                    TripReportPdfMetrics.A4_WIDTH_PT,
                    TripReportPdfMetrics.A4_HEIGHT_PT,
                    pageNumber
                )
                val pageLinks = renderPage(
                    canvas = canvas,
                    blocks = blocks,
                    paints = paints,
                    brandFooter = payload.brandFooter,
                    footerText = formatFooter(payload.pageFooterTemplate, pageNumber, totalPages)
                )
                if (pageLinks.isNotEmpty()) {
                    linksByPage[index] = pageLinks
                }
                session.finishCurrentPage()
            }
            val rawPdfBytes = ByteArrayOutputStream().use { buffer ->
                session.writeTo(buffer)
                buffer.toByteArray()
            }
            val annotatedPdfBytes = TripReportPdfLinkAnnotator.appendLinkAnnotations(
                pdfBytes = rawPdfBytes,
                linksByPage = linksByPage,
                pageHeightPt = TripReportPdfMetrics.A4_HEIGHT_PT.toFloat()
            )
            outputFile.parentFile?.mkdirs()
            FileOutputStream(outputFile).use { out ->
                out.write(annotatedPdfBytes)
            }
        } finally {
            session.close()
        }
    }

    internal fun buildPagePlans(
        payload: TripReportExportPayload,
        paints: TripReportPdfPaints = TripReportPdfTheme.createPaints(context)
    ): List<List<PdfBlock>> {
        val allBlocks = flattenPayload(payload, paints)
        if (allBlocks.isEmpty()) return listOf(emptyList())

        val availableHeight = TripReportPdfMetrics.MAX_CONTENT_Y_PT - TripReportPdfMetrics.MARGIN_TOP_PT
        val paginator = HeightPaginator(availableHeight)
        allBlocks.forEach(paginator::accept)
        return paginator.finish()
    }

    private fun flattenPayload(
        payload: TripReportExportPayload,
        paints: TripReportPdfPaints
    ): List<PdfBlock> = buildList {
        add(
            PdfBlock.BrandHeader(
                brandTagline = payload.brandFooter.brandTagline,
                periodLabel = payload.periodLabel
            )
        )
        val heroHeight = TripReportPdfBrandDrawers.measureHeroGroupCardHeight(payload.groupDescription, paints) + 10f
        val metaLine = "${payload.generatedAtLabel}  ·  ${payload.baseCurrency}"
        add(
            PdfBlock.HeroGroupCard(
                reportTitle = payload.reportTitle,
                groupName = payload.groupName,
                groupDescription = payload.groupDescription,
                metaLine = metaLine,
                heightPt = heroHeight
            )
        )
        if (payload.summaryMetrics.isNotEmpty()) {
            add(
                PdfBlock.SummaryMetricsGrid(
                    metrics = payload.summaryMetrics,
                    heightPt = TripReportPdfBrandDrawers.measureSummaryGridHeight(payload.summaryMetrics)
                )
            )
        }

        appendTableSection(this, payload.memberBalancesSection, payload.emptySectionLabel)
        payload.subunitBalancesSection?.let { appendTableSection(this, it, payload.emptySectionLabel) }
        appendTableSection(this, payload.settlementsSection, payload.emptySectionLabel)

        val upsellCallout = payload.proUpsellCallout
        if (upsellCallout != null) {
            add(PdfBlock.Spacer)
            add(PdfBlock.ProUpsellCard(upsellCallout))
        } else {
            payload.expensesSection?.let { appendTableSection(this, it, payload.emptySectionLabel) }
            payload.contributionsSection?.let { appendTableSection(this, it, payload.emptySectionLabel) }
            payload.withdrawalsSection?.let { appendTableSection(this, it, payload.emptySectionLabel) }
        }

        add(PdfBlock.Spacer)
        add(PdfBlock.BrandFooterCard(payload.brandFooter))
    }

    private fun appendTableSection(
        blocks: MutableList<PdfBlock>,
        section: TripReportTableSection,
        emptySectionLabel: String
    ) {
        blocks.add(PdfBlock.Spacer)
        blocks.add(PdfBlock.SectionHeading(title = section.title, accentTone = section.accentTone))
        if (section.headers.isNotEmpty()) {
            blocks.add(
                PdfBlock.TableHeader(
                    headers = section.headers,
                    columnWeights = section.columnWeights,
                    columnAlignments = section.columnAlignments
                )
            )
        }
        if (section.rows.isEmpty()) {
            blocks.add(PdfBlock.EmptySection(emptySectionLabel))
        } else {
            section.rows.forEachIndexed { rowIndex, row ->
                blocks.add(
                    PdfBlock.TableRow(
                        cells = row,
                        columnWeights = section.columnWeights,
                        columnAlignments = section.columnAlignments,
                        rowIndex = rowIndex
                    )
                )
            }
        }
    }

    private fun renderPage(
        canvas: Canvas,
        blocks: List<PdfBlock>,
        paints: TripReportPdfPaints,
        brandFooter: TripReportBrandFooter,
        footerText: String
    ): List<PdfLinkAnnotation> {
        canvas.drawRect(
            0f,
            0f,
            TripReportPdfMetrics.A4_WIDTH_PT.toFloat(),
            TripReportPdfMetrics.A4_HEIGHT_PT.toFloat(),
            paints.pageBg
        )

        val links = mutableListOf<PdfLinkAnnotation>()
        var cursorY = TripReportPdfMetrics.MARGIN_TOP_PT
        blocks.forEach { block ->
            cursorY = drawBlock(canvas, block, paints, cursorY, links)
        }

        TripReportPdfBrandDrawers.drawRunningPageFooter(
            canvas = canvas,
            websiteDisplay = brandFooter.websiteDisplay,
            websiteUrl = brandFooter.websiteUrl,
            pageFooterText = footerText,
            paints = paints,
            links = links
        )
        return links
    }

    private fun drawBlock(
        canvas: Canvas,
        block: PdfBlock,
        paints: TripReportPdfPaints,
        cursorY: Float,
        links: MutableList<PdfLinkAnnotation>
    ): Float = when (block) {
        is PdfBlock.BrandHeader,
        is PdfBlock.HeroGroupCard,
        is PdfBlock.SummaryMetricsGrid,
        is PdfBlock.ProUpsellCard,
        is PdfBlock.BrandFooterCard -> drawEditorialBlock(canvas, block, paints, cursorY, links)
        is PdfBlock.SectionHeading,
        is PdfBlock.TableHeader,
        is PdfBlock.TableRow,
        is PdfBlock.EmptySection -> drawSectionBlock(canvas, block, paints, cursorY)
        PdfBlock.Spacer -> cursorY + TripReportPdfMetrics.SPACER_HEIGHT_PT
    }

    private fun drawEditorialBlock(
        canvas: Canvas,
        block: PdfBlock,
        paints: TripReportPdfPaints,
        cursorY: Float,
        links: MutableList<PdfLinkAnnotation>
    ): Float = when (block) {
        is PdfBlock.BrandHeader -> TripReportPdfBrandDrawers.drawBrandHeader(
            canvas,
            block.brandTagline,
            block.periodLabel,
            paints,
            cursorY
        )
        is PdfBlock.HeroGroupCard -> TripReportPdfBrandDrawers.drawHeroGroupCard(
            canvas,
            block.reportTitle,
            block.groupName,
            block.groupDescription,
            block.metaLine,
            paints,
            cursorY
        )
        is PdfBlock.SummaryMetricsGrid -> TripReportPdfBrandDrawers.drawSummaryMetricsGrid(
            canvas,
            block.metrics,
            paints,
            cursorY
        )
        is PdfBlock.ProUpsellCard -> TripReportPdfBrandDrawers.drawProUpsellCard(
            canvas,
            block.callout,
            paints,
            cursorY
        )
        is PdfBlock.BrandFooterCard -> TripReportPdfBrandDrawers.drawBrandFooterCard(
            canvas,
            block.brandFooter,
            paints,
            cursorY,
            links
        )
        else -> cursorY
    }

    private fun drawSectionBlock(
        canvas: Canvas,
        block: PdfBlock,
        paints: TripReportPdfPaints,
        cursorY: Float
    ): Float = when (block) {
        is PdfBlock.SectionHeading -> TripReportPdfSectionDrawers.drawSectionHeading(
            canvas,
            block.title,
            block.accentTone,
            paints,
            cursorY
        )
        is PdfBlock.TableHeader -> TripReportPdfSectionDrawers.drawTableHeader(
            canvas,
            block.headers,
            block.columnWeights,
            block.columnAlignments,
            paints,
            cursorY
        )
        is PdfBlock.TableRow -> TripReportPdfSectionDrawers.drawTableRow(
            canvas,
            block.cells,
            block.columnWeights,
            block.columnAlignments,
            block.rowIndex,
            paints,
            cursorY
        )
        is PdfBlock.EmptySection -> TripReportPdfSectionDrawers.drawEmptySectionCard(
            canvas,
            block.text,
            paints,
            cursorY
        )
        else -> cursorY
    }

    private fun formatFooter(template: String, pageNumber: Int, totalPages: Int): String =
        runCatching { String.format(template, pageNumber, totalPages) }
            .getOrElse { "$pageNumber / $totalPages" }

    private class HeightPaginator(private val maxHeightPt: Float) {
        private val pages = mutableListOf<List<PdfBlock>>()
        private var currentPage = mutableListOf<PdfBlock>()
        private var usedHeightPt = 0f
        private var currentSectionHeading: PdfBlock.SectionHeading? = null
        private var currentTableHeader: PdfBlock.TableHeader? = null

        fun accept(block: PdfBlock) {
            if (currentPage.isEmpty() && block is PdfBlock.Spacer) return
            updateActiveHeaders(block)

            val requiredHeight = requiredHeightWithLookahead(block)
            if (currentPage.isNotEmpty() && usedHeightPt + requiredHeight > maxHeightPt) {
                flushPage(nextBlock = block)
            }

            if (currentPage.isNotEmpty() || block !is PdfBlock.Spacer) {
                currentPage.add(block)
                usedHeightPt += block.heightPt
            }
        }

        fun finish(): List<List<PdfBlock>> {
            if (currentPage.isNotEmpty()) {
                pages.add(currentPage)
            }
            return pages
        }

        private fun requiredHeightWithLookahead(block: PdfBlock): Float = when (block) {
            // Keep section heading together with at least its table header and one row
            is PdfBlock.SectionHeading ->
                block.heightPt +
                    TripReportPdfMetrics.TABLE_HEADER_HEIGHT_PT +
                    TripReportPdfMetrics.TABLE_ROW_HEIGHT_PT
            is PdfBlock.TableHeader -> block.heightPt + TripReportPdfMetrics.TABLE_ROW_HEIGHT_PT
            else -> block.heightPt
        }

        private fun updateActiveHeaders(block: PdfBlock) {
            when (block) {
                is PdfBlock.SectionHeading -> {
                    currentSectionHeading = block
                    currentTableHeader = null
                }
                is PdfBlock.TableHeader -> currentTableHeader = block
                is PdfBlock.ProUpsellCard,
                is PdfBlock.BrandFooterCard -> {
                    currentSectionHeading = null
                    currentTableHeader = null
                }
                else -> Unit
            }
        }

        private fun flushPage(nextBlock: PdfBlock) {
            pages.add(currentPage)
            currentPage = mutableListOf()
            usedHeightPt = 0f
            if (nextBlock is PdfBlock.TableRow) {
                currentSectionHeading?.let { heading ->
                    currentPage.add(heading)
                    usedHeightPt += heading.heightPt
                }
                currentTableHeader?.let { header ->
                    currentPage.add(header)
                    usedHeightPt += header.heightPt
                }
            }
        }
    }
}
