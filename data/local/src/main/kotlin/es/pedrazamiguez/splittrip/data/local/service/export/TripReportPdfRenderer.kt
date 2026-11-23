package es.pedrazamiguez.splittrip.data.local.service.export

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import es.pedrazamiguez.splittrip.domain.model.report.TripReportExportPayload
import es.pedrazamiguez.splittrip.domain.model.report.TripReportTableSection
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

internal data class PdfLineItem(
    val type: PdfLineType,
    val columns: List<String>
)

internal enum class PdfLineType {
    TITLE,
    SUBTITLE,
    SECTION_HEADER,
    KEY_VALUE,
    TABLE_HEADER,
    TABLE_ROW,
    EMPTY_HINT,
    SPACER
}

class TripReportPdfRenderer internal constructor(
    private val sessionFactory: () -> PdfDocumentSession
) {

    constructor() : this(sessionFactory = { AndroidPdfDocumentSession() })

    fun render(payload: TripReportExportPayload, outputFile: File) {
        val pagePlans = buildPagePlans(payload)
        val totalPages = pagePlans.size.coerceAtLeast(1)
        val session = sessionFactory()
        val paints = createPaints()

        try {
            pagePlans.forEachIndexed { index, items ->
                val pageNumber = index + 1
                val canvas = session.startPage(A4_WIDTH_PT, A4_HEIGHT_PT, pageNumber)
                renderPage(
                    canvas = canvas,
                    items = items,
                    paints = paints,
                    footerText = formatFooter(payload.pageFooterTemplate, pageNumber, totalPages)
                )
                session.finishCurrentPage()
            }
            outputFile.parentFile?.mkdirs()
            FileOutputStream(outputFile).use { out ->
                session.writeTo(out)
            }
        } finally {
            session.close()
        }
    }

    internal fun buildPagePlans(
        payload: TripReportExportPayload,
        maxLinesPerPage: Int = DEFAULT_MAX_LINES_PER_PAGE
    ): List<List<PdfLineItem>> {
        val effectiveLimit = maxLinesPerPage.coerceAtLeast(MIN_LINES_PER_PAGE)
        val allLines = flattenPayload(payload)
        if (allLines.isEmpty()) return listOf(emptyList())

        val accumulator = PagePlanAccumulator(effectiveLimit)
        allLines.forEach(accumulator::accept)
        return accumulator.finish()
    }

    private class PagePlanAccumulator(private val maxLinesPerPage: Int) {
        private val pages = mutableListOf<List<PdfLineItem>>()
        private var currentPage = mutableListOf<PdfLineItem>()
        private var currentSectionHeader: PdfLineItem? = null
        private var currentTableHeader: PdfLineItem? = null

        fun accept(item: PdfLineItem) {
            if (currentPage.isEmpty() && item.type == PdfLineType.SPACER) return
            updateHeaders(item)
            if (currentPage.size >= maxLinesPerPage) {
                flushPage(nextItem = item)
            }
            if (currentPage.isNotEmpty() || item.type != PdfLineType.SPACER) {
                currentPage.add(item)
            }
        }

        fun finish(): List<List<PdfLineItem>> {
            if (currentPage.isNotEmpty()) {
                pages.add(currentPage)
            }
            return pages
        }

        private fun updateHeaders(item: PdfLineItem) {
            when (item.type) {
                PdfLineType.SECTION_HEADER -> {
                    currentSectionHeader = item
                    currentTableHeader = null
                }
                PdfLineType.TABLE_HEADER -> currentTableHeader = item
                else -> Unit
            }
        }

        private fun flushPage(nextItem: PdfLineItem) {
            pages.add(currentPage)
            currentPage = mutableListOf()
            if (nextItem.type == PdfLineType.TABLE_ROW) {
                currentSectionHeader?.let(currentPage::add)
                currentTableHeader?.let(currentPage::add)
            }
        }
    }

    private fun flattenPayload(payload: TripReportExportPayload): List<PdfLineItem> {
        val lines = mutableListOf<PdfLineItem>()
        lines.add(PdfLineItem(PdfLineType.TITLE, listOf(payload.reportTitle)))
        lines.add(PdfLineItem(PdfLineType.SUBTITLE, listOf(payload.groupName)))
        if (payload.groupDescription.isNotBlank()) {
            lines.add(PdfLineItem(PdfLineType.SUBTITLE, listOf(payload.groupDescription)))
        }
        lines.add(
            PdfLineItem(
                PdfLineType.SUBTITLE,
                listOf("${payload.periodLabel}  |  ${payload.generatedAtLabel}")
            )
        )
        lines.add(PdfLineItem(PdfLineType.SPACER, emptyList()))

        lines.add(PdfLineItem(PdfLineType.SECTION_HEADER, listOf(payload.summarySectionTitle)))
        payload.summaryMetrics.forEach { metric ->
            lines.add(PdfLineItem(PdfLineType.KEY_VALUE, listOf(metric.label, metric.value)))
        }

        appendTableSection(lines, payload.memberBalancesSection, payload.emptySectionLabel)
        payload.subunitBalancesSection?.let { appendTableSection(lines, it, payload.emptySectionLabel) }
        appendTableSection(lines, payload.settlementsSection, payload.emptySectionLabel)
        appendTableSection(lines, payload.expensesSection, payload.emptySectionLabel)
        appendTableSection(lines, payload.contributionsSection, payload.emptySectionLabel)
        appendTableSection(lines, payload.withdrawalsSection, payload.emptySectionLabel)
        return lines
    }

    private fun appendTableSection(
        lines: MutableList<PdfLineItem>,
        section: TripReportTableSection,
        emptySectionLabel: String
    ) {
        lines.add(PdfLineItem(PdfLineType.SPACER, emptyList()))
        lines.add(PdfLineItem(PdfLineType.SECTION_HEADER, listOf(section.title)))
        if (section.headers.isNotEmpty()) {
            lines.add(PdfLineItem(PdfLineType.TABLE_HEADER, section.headers))
        }
        if (section.rows.isEmpty()) {
            lines.add(PdfLineItem(PdfLineType.EMPTY_HINT, listOf(emptySectionLabel)))
        } else {
            section.rows.forEach { row ->
                lines.add(PdfLineItem(PdfLineType.TABLE_ROW, row))
            }
        }
    }

    private fun renderPage(
        canvas: Canvas,
        items: List<PdfLineItem>,
        paints: PdfPaints,
        footerText: String
    ) {
        var cursorY = MARGIN_TOP_PT
        items.forEach { item ->
            cursorY = drawLineItem(canvas, item, paints, cursorY)
        }

        val footerY = A4_HEIGHT_PT - MARGIN_BOTTOM_PT
        canvas.drawLine(
            MARGIN_X_PT,
            footerY - FOOTER_RULE_OFFSET_PT,
            RIGHT_EDGE_PT,
            footerY - FOOTER_RULE_OFFSET_PT,
            paints.divider
        )
        canvas.drawText(footerText, MARGIN_X_PT, footerY, paints.footer)
    }

    private fun drawLineItem(
        canvas: Canvas,
        item: PdfLineItem,
        paints: PdfPaints,
        cursorY: Float
    ): Float = when (item.type) {
        PdfLineType.TITLE -> {
            canvas.drawText(
                fitText(item.columns.firstOrNull().orEmpty(), CONTENT_WIDTH_PT, paints.title),
                MARGIN_X_PT,
                cursorY,
                paints.title
            )
            cursorY + TITLE_STEP_PT
        }
        PdfLineType.SUBTITLE,
        PdfLineType.EMPTY_HINT -> {
            canvas.drawText(
                fitText(item.columns.firstOrNull().orEmpty(), CONTENT_WIDTH_PT, paints.subtitle),
                MARGIN_X_PT,
                cursorY,
                paints.subtitle
            )
            if (item.type == PdfLineType.SUBTITLE) cursorY + SUBTITLE_STEP_PT else cursorY + ROW_STEP_PT
        }
        PdfLineType.SECTION_HEADER -> drawSectionHeader(canvas, item, paints, cursorY)
        PdfLineType.KEY_VALUE -> drawKeyValueRow(canvas, item, paints, cursorY)
        PdfLineType.TABLE_HEADER -> drawTableHeader(canvas, item, paints, cursorY)
        PdfLineType.TABLE_ROW -> {
            drawTableColumns(canvas, item.columns, paints.tableCell, cursorY)
            cursorY + ROW_STEP_PT
        }
        PdfLineType.SPACER -> cursorY + SPACER_STEP_PT
    }

    private fun drawSectionHeader(
        canvas: Canvas,
        item: PdfLineItem,
        paints: PdfPaints,
        cursorY: Float
    ): Float {
        canvas.drawRect(
            MARGIN_X_PT,
            cursorY - SECTION_BG_TOP_OFFSET_PT,
            RIGHT_EDGE_PT,
            cursorY + SECTION_BG_BOTTOM_OFFSET_PT,
            paints.sectionBg
        )
        canvas.drawText(
            fitText(item.columns.firstOrNull().orEmpty(), CONTENT_WIDTH_PT - CELL_PAD_PT, paints.sectionHeader),
            MARGIN_X_PT + CELL_PAD_PT,
            cursorY,
            paints.sectionHeader
        )
        return cursorY + SECTION_STEP_PT
    }

    private fun drawKeyValueRow(
        canvas: Canvas,
        item: PdfLineItem,
        paints: PdfPaints,
        cursorY: Float
    ): Float {
        val label = item.columns.getOrNull(0).orEmpty()
        val value = item.columns.getOrNull(1).orEmpty()
        canvas.drawText(
            fitText(label, KEY_VALUE_LABEL_WIDTH_PT, paints.body),
            MARGIN_X_PT,
            cursorY,
            paints.body
        )
        canvas.drawText(
            fitText(value, CONTENT_WIDTH_PT - KEY_VALUE_LABEL_WIDTH_PT, paints.boldBody),
            MARGIN_X_PT + KEY_VALUE_LABEL_WIDTH_PT,
            cursorY,
            paints.boldBody
        )
        return cursorY + ROW_STEP_PT
    }

    private fun drawTableHeader(
        canvas: Canvas,
        item: PdfLineItem,
        paints: PdfPaints,
        cursorY: Float
    ): Float {
        drawTableColumns(canvas, item.columns, paints.tableHeader, cursorY)
        canvas.drawLine(
            MARGIN_X_PT,
            cursorY + HEADER_RULE_OFFSET_PT,
            RIGHT_EDGE_PT,
            cursorY + HEADER_RULE_OFFSET_PT,
            paints.divider
        )
        return cursorY + ROW_STEP_PT
    }

    private fun drawTableColumns(
        canvas: Canvas,
        columns: List<String>,
        paint: Paint,
        cursorY: Float
    ) {
        if (columns.isEmpty()) return
        val colWidth = CONTENT_WIDTH_PT / columns.size.toFloat()
        columns.forEachIndexed { colIndex, text ->
            val x = MARGIN_X_PT + colIndex * colWidth
            val maxTextWidth = (colWidth - CELL_PAD_PT).coerceAtLeast(MIN_COLUMN_WIDTH_PT)
            canvas.drawText(fitText(text, maxTextWidth, paint), x, cursorY, paint)
        }
    }

    private fun fitText(text: String, maxWidth: Float, paint: Paint): String {
        if (text.isEmpty() || paint.measureText(text) <= maxWidth) return text
        val ellipsisWidth = paint.measureText(ELLIPSIS)
        val available = (maxWidth - ellipsisWidth).coerceAtLeast(0f)
        val count = paint.breakText(text, true, available, null)
        return text.take(count) + ELLIPSIS
    }

    private fun formatFooter(template: String, pageNumber: Int, totalPages: Int): String =
        runCatching { String.format(template, pageNumber, totalPages) }
            .getOrElse { "$pageNumber / $totalPages" }

    private fun createPaints(): PdfPaints = PdfPaints(
        title = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(COLOR_PRIMARY_INK)
            textSize = TITLE_TEXT_SIZE_PT
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        },
        subtitle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(COLOR_MUTED_INK)
            textSize = SUBTITLE_TEXT_SIZE_PT
        },
        sectionHeader = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(COLOR_PRIMARY_INK)
            textSize = SECTION_TEXT_SIZE_PT
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        },
        sectionBg = Paint().apply {
            color = Color.parseColor(COLOR_SECTION_BG)
            style = Paint.Style.FILL
        },
        body = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(COLOR_BODY_INK)
            textSize = BODY_TEXT_SIZE_PT
        },
        boldBody = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(COLOR_PRIMARY_INK)
            textSize = BODY_TEXT_SIZE_PT
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        },
        tableHeader = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(COLOR_MUTED_INK)
            textSize = TABLE_TEXT_SIZE_PT
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        },
        tableCell = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(COLOR_BODY_INK)
            textSize = TABLE_TEXT_SIZE_PT
        },
        divider = Paint().apply {
            color = Color.parseColor(COLOR_DIVIDER)
            strokeWidth = DIVIDER_WIDTH_PT
        },
        footer = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(COLOR_MUTED_INK)
            textSize = FOOTER_TEXT_SIZE_PT
        }
    )

    private data class PdfPaints(
        val title: Paint,
        val subtitle: Paint,
        val sectionHeader: Paint,
        val sectionBg: Paint,
        val body: Paint,
        val boldBody: Paint,
        val tableHeader: Paint,
        val tableCell: Paint,
        val divider: Paint,
        val footer: Paint
    )

    private companion object {
        const val A4_WIDTH_PT = 595
        const val A4_HEIGHT_PT = 842
        const val DEFAULT_MAX_LINES_PER_PAGE = 42
        const val MIN_LINES_PER_PAGE = 10
        const val MARGIN_X_PT = 36f
        const val MARGIN_TOP_PT = 44f
        const val MARGIN_BOTTOM_PT = 28f
        const val RIGHT_EDGE_PT = A4_WIDTH_PT - MARGIN_X_PT
        const val CONTENT_WIDTH_PT = RIGHT_EDGE_PT - MARGIN_X_PT
        const val KEY_VALUE_LABEL_WIDTH_PT = 240f
        const val CELL_PAD_PT = 6f
        const val MIN_COLUMN_WIDTH_PT = 16f

        const val TITLE_TEXT_SIZE_PT = 16f
        const val SUBTITLE_TEXT_SIZE_PT = 9.5f
        const val SECTION_TEXT_SIZE_PT = 11f
        const val BODY_TEXT_SIZE_PT = 9.5f
        const val TABLE_TEXT_SIZE_PT = 8f
        const val FOOTER_TEXT_SIZE_PT = 8.5f

        const val TITLE_STEP_PT = 20f
        const val SUBTITLE_STEP_PT = 14f
        const val SECTION_STEP_PT = 20f
        const val ROW_STEP_PT = 16f
        const val SPACER_STEP_PT = 10f
        const val SECTION_BG_TOP_OFFSET_PT = 11f
        const val SECTION_BG_BOTTOM_OFFSET_PT = 5f
        const val HEADER_RULE_OFFSET_PT = 4f
        const val FOOTER_RULE_OFFSET_PT = 12f
        const val DIVIDER_WIDTH_PT = 0.75f

        const val COLOR_PRIMARY_INK = "#111827"
        const val COLOR_BODY_INK = "#1F2937"
        const val COLOR_MUTED_INK = "#4B5563"
        const val COLOR_SECTION_BG = "#EEF2F7"
        const val COLOR_DIVIDER = "#D1D5DB"
        const val ELLIPSIS = "..."
    }
}
