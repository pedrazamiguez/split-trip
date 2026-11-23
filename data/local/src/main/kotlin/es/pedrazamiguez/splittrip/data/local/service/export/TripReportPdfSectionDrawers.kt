@file:Suppress("MagicNumber")

package es.pedrazamiguez.splittrip.data.local.service.export

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import es.pedrazamiguez.splittrip.domain.model.report.TripReportCell
import es.pedrazamiguez.splittrip.domain.model.report.TripReportCellAlign
import es.pedrazamiguez.splittrip.domain.model.report.TripReportTone

internal object TripReportPdfSectionDrawers {

    private const val ACCENT_BAR_WIDTH_PT = 3.5f
    private const val ACCENT_BAR_HEIGHT_PT = 13f
    private const val BADGE_HEIGHT_PT = 14f
    private const val BADGE_PAD_X_PT = 6f

    fun drawSectionHeading(
        canvas: Canvas,
        title: String,
        accentTone: TripReportTone,
        paints: TripReportPdfPaints,
        topY: Float
    ): Float {
        val leftX = TripReportPdfMetrics.MARGIN_X_PT
        paints.accentFill.color = TripReportPdfTheme.resolveToneColor(accentTone)
        val barRect = RectF(leftX, topY + 4f, leftX + ACCENT_BAR_WIDTH_PT, topY + 4f + ACCENT_BAR_HEIGHT_PT)
        canvas.drawRoundRect(barRect, 2f, 2f, paints.accentFill)

        val fittedTitle = TripReportPdfTheme.fitText(
            text = title,
            maxWidth = TripReportPdfMetrics.CONTENT_WIDTH_PT - 14f,
            paint = paints.sectionTitle
        )
        canvas.drawText(fittedTitle, leftX + 10f, topY + 15f, paints.sectionTitle)
        return topY + TripReportPdfMetrics.SECTION_HEADING_HEIGHT_PT
    }

    fun drawTableHeader(
        canvas: Canvas,
        headers: List<String>,
        columnWeights: List<Float>,
        columnAlignments: List<TripReportCellAlign>,
        paints: TripReportPdfPaints,
        topY: Float
    ): Float {
        val headerRect = RectF(
            TripReportPdfMetrics.MARGIN_X_PT,
            topY,
            TripReportPdfMetrics.RIGHT_EDGE_PT,
            topY + TripReportPdfMetrics.TABLE_HEADER_HEIGHT_PT
        )
        canvas.drawRoundRect(headerRect, 6f, 6f, paints.cardInset)

        val widths = resolveColumnWidths(headers.size, columnWeights)
        var cursorX = TripReportPdfMetrics.MARGIN_X_PT + TripReportPdfMetrics.TABLE_INSET_PT
        val baselineY = topY + 15f

        headers.forEachIndexed { index, headerText ->
            val colWidth = widths[index]
            val align = columnAlignments.getOrNull(index) ?: TripReportCellAlign.START
            val maxTextWidth = (colWidth - TripReportPdfMetrics.CELL_PAD_PT)
                .coerceAtLeast(TripReportPdfMetrics.MIN_COLUMN_WIDTH_PT)
            val fitted = TripReportPdfTheme.fitText(headerText, maxTextWidth, paints.tableHeader)
            val drawX = resolveAlignedX(cursorX, colWidth, fitted, paints.tableHeader, align)
            canvas.drawText(fitted, drawX, baselineY, paints.tableHeader)
            cursorX += colWidth
        }

        return topY + TripReportPdfMetrics.TABLE_HEADER_HEIGHT_PT
    }

    @Suppress("LongParameterList")
    fun drawTableRow(
        canvas: Canvas,
        cells: List<TripReportCell>,
        columnWeights: List<Float>,
        columnAlignments: List<TripReportCellAlign>,
        rowIndex: Int,
        paints: TripReportPdfPaints,
        topY: Float
    ): Float {
        val rowRect = RectF(
            TripReportPdfMetrics.MARGIN_X_PT,
            topY,
            TripReportPdfMetrics.RIGHT_EDGE_PT,
            topY + TripReportPdfMetrics.TABLE_ROW_HEIGHT_PT
        )
        val bgPaint = if (rowIndex % 2 == 0) paints.cardWhite else paints.rowAlt
        canvas.drawRect(rowRect, bgPaint)

        val widths = resolveColumnWidths(cells.size, columnWeights)
        var cursorX = TripReportPdfMetrics.MARGIN_X_PT + TripReportPdfMetrics.TABLE_INSET_PT
        val baselineY = topY + 14.2f
        val centerY = topY + TripReportPdfMetrics.TABLE_ROW_HEIGHT_PT / 2f

        cells.forEachIndexed { index, cell ->
            val colWidth = widths[index]
            val align = columnAlignments.getOrNull(index) ?: cell.align
            val badgeColors = TripReportPdfTheme.resolveBadgeColors(cell.tone)
            if (badgeColors != null) {
                drawBadgeCell(
                    canvas = canvas,
                    text = cell.text,
                    colStartX = cursorX,
                    colWidth = colWidth,
                    centerY = centerY,
                    bgColor = badgeColors.backgroundColor,
                    textColor = badgeColors.textColor,
                    align = align,
                    paints = paints
                )
            } else {
                drawPlainCell(
                    canvas = canvas,
                    cell = cell,
                    colStartX = cursorX,
                    colWidth = colWidth,
                    baselineY = baselineY,
                    align = align,
                    paints = paints
                )
            }
            cursorX += colWidth
        }

        return topY + TripReportPdfMetrics.TABLE_ROW_HEIGHT_PT
    }

    fun drawEmptySectionCard(
        canvas: Canvas,
        emptyText: String,
        paints: TripReportPdfPaints,
        topY: Float
    ): Float {
        val rect = RectF(
            TripReportPdfMetrics.MARGIN_X_PT,
            topY,
            TripReportPdfMetrics.RIGHT_EDGE_PT,
            topY + TripReportPdfMetrics.EMPTY_SECTION_HEIGHT_PT
        )
        canvas.drawRoundRect(
            rect,
            TripReportPdfMetrics.SMALL_CARD_RADIUS_PT,
            TripReportPdfMetrics.SMALL_CARD_RADIUS_PT,
            paints.cardWhite
        )
        canvas.drawRoundRect(
            rect,
            TripReportPdfMetrics.SMALL_CARD_RADIUS_PT,
            TripReportPdfMetrics.SMALL_CARD_RADIUS_PT,
            paints.ghostStroke
        )
        val fitted = TripReportPdfTheme.fitText(emptyText, rect.width() - 24f, paints.heroMeta)
        canvas.drawText(fitted, rect.left + 14f, topY + 20f, paints.heroMeta)
        return topY + TripReportPdfMetrics.EMPTY_SECTION_HEIGHT_PT
    }

    private fun drawPlainCell(
        canvas: Canvas,
        cell: TripReportCell,
        colStartX: Float,
        colWidth: Float,
        baselineY: Float,
        align: TripReportCellAlign,
        paints: TripReportPdfPaints
    ) {
        paints.tableCell.color = TripReportPdfTheme.resolveToneColor(cell.tone)
        paints.tableCell.typeface = TripReportPdfTheme.resolveToneTypeface(cell.tone, paints.typefaces)
        val maxTextWidth = (colWidth - TripReportPdfMetrics.CELL_PAD_PT)
            .coerceAtLeast(TripReportPdfMetrics.MIN_COLUMN_WIDTH_PT)
        val fitted = TripReportPdfTheme.fitText(cell.text, maxTextWidth, paints.tableCell)
        val drawX = resolveAlignedX(colStartX, colWidth, fitted, paints.tableCell, align)
        canvas.drawText(fitted, drawX, baselineY, paints.tableCell)
    }

    @Suppress("LongParameterList")
    private fun drawBadgeCell(
        canvas: Canvas,
        text: String,
        colStartX: Float,
        colWidth: Float,
        centerY: Float,
        bgColor: Int,
        textColor: Int,
        align: TripReportCellAlign,
        paints: TripReportPdfPaints
    ) {
        val maxBadgeWidth = (colWidth - 4f).coerceAtLeast(TripReportPdfMetrics.MIN_COLUMN_WIDTH_PT)
        val fitted = TripReportPdfTheme.fitText(text, maxBadgeWidth - (BADGE_PAD_X_PT * 2f), paints.badgeText)
        val textWidth = paints.badgeText.measureText(fitted)
        val pillWidth = (textWidth + BADGE_PAD_X_PT * 2f).coerceAtMost(maxBadgeWidth)
        val pillLeft = when (align) {
            TripReportCellAlign.START -> colStartX
            TripReportCellAlign.END -> (colStartX + colWidth - TripReportPdfMetrics.CELL_PAD_PT - pillWidth)
                .coerceAtLeast(colStartX)
        }
        val pillRect = RectF(
            pillLeft,
            centerY - BADGE_HEIGHT_PT / 2f,
            pillLeft + pillWidth,
            centerY + BADGE_HEIGHT_PT / 2f
        )
        paints.badgeBg.color = bgColor
        paints.badgeText.color = textColor
        canvas.drawRoundRect(
            pillRect,
            TripReportPdfMetrics.PILL_RADIUS_PT,
            TripReportPdfMetrics.PILL_RADIUS_PT,
            paints.badgeBg
        )
        canvas.drawText(fitted, pillRect.left + BADGE_PAD_X_PT, centerY + 2.5f, paints.badgeText)
    }

    private fun resolveAlignedX(
        colStartX: Float,
        colWidth: Float,
        fittedText: String,
        paint: Paint,
        align: TripReportCellAlign
    ): Float = when (align) {
        TripReportCellAlign.START -> colStartX
        TripReportCellAlign.END -> {
            val textWidth = paint.measureText(fittedText)
            val rightEdge = colStartX + colWidth - TripReportPdfMetrics.CELL_PAD_PT
            (rightEdge - textWidth).coerceAtLeast(colStartX)
        }
    }

    internal fun resolveColumnWidths(columnCount: Int, weights: List<Float>): List<Float> {
        if (columnCount <= 0) return emptyList()
        val totalWidth = TripReportPdfMetrics.TABLE_INNER_WIDTH_PT
        if (weights.size == columnCount && weights.all { it > 0f }) {
            val weightSum = weights.sum()
            return weights.map { weight -> (weight / weightSum) * totalWidth }
        }
        val uniform = totalWidth / columnCount.toFloat()
        return List(columnCount) { uniform }
    }
}
