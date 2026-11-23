@file:Suppress("MagicNumber")

package es.pedrazamiguez.splittrip.data.local.service.export

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import es.pedrazamiguez.splittrip.domain.model.report.TripReportBrandFooter
import es.pedrazamiguez.splittrip.domain.model.report.TripReportMetricCard
import es.pedrazamiguez.splittrip.domain.model.report.TripReportProUpsellCallout
import java.util.Locale

internal object TripReportPdfBrandDrawers {

    private const val HEADER_LOGO_SIZE_PT = 28f
    private const val FOOTER_LOGO_SIZE_PT = 24f
    private const val PERIOD_PILL_HEIGHT_PT = 20f
    private const val PERIOD_DOT_RADIUS_PT = 2.6f
    private const val HERO_STRIPE_HEIGHT_PT = 4f
    private const val KPI_GAP_PT = 8f
    private const val KPI_HEIGHT_PT = 54f
    private const val KPI_ACCENT_WIDTH_PT = 3.5f
    private const val MAX_HERO_DESC_LINES = 2
    private const val MAX_UPSELL_BODY_LINES = 3
    private const val MAX_CTA_SUBTITLE_LINES = 2

    fun drawBrandHeader(
        canvas: Canvas,
        brandTagline: String,
        periodLabel: String,
        paints: TripReportPdfPaints,
        topY: Float
    ): Float {
        val leftX = TripReportPdfMetrics.MARGIN_X_PT
        val logoCenterY = topY + HEADER_LOGO_SIZE_PT / 2f

        val logoCircle = RectF(leftX, topY, leftX + HEADER_LOGO_SIZE_PT, topY + HEADER_LOGO_SIZE_PT)
        canvas.drawOval(logoCircle, paints.cardWhite)
        canvas.drawOval(logoCircle, paints.ghostStroke)
        TripReportPdfGraphics.drawBrandLogoVector(
            canvas = canvas,
            left = leftX + 4f,
            top = topY + 4f,
            size = HEADER_LOGO_SIZE_PT - 8f
        )

        val wordmarkX = leftX + HEADER_LOGO_SIZE_PT + 8f
        val splitWidth = paints.brandSplit.measureText("Split")
        val wordmarkBaseline = topY + 14.5f
        canvas.drawText("Split", wordmarkX, wordmarkBaseline, paints.brandSplit)
        canvas.drawText("Trip", wordmarkX + splitWidth, wordmarkBaseline, paints.brandTrip)

        val tagline = TripReportPdfTheme.fitText(brandTagline, 210f, paints.brandTagline)
        canvas.drawText(tagline, wordmarkX, topY + 25f, paints.brandTagline)

        drawPeriodPill(
            canvas = canvas,
            periodLabel = periodLabel,
            centerY = logoCenterY,
            paints = paints
        )

        return topY + TripReportPdfMetrics.BRAND_HEADER_HEIGHT_PT
    }

    private fun drawPeriodPill(
        canvas: Canvas,
        periodLabel: String,
        centerY: Float,
        paints: TripReportPdfPaints
    ) {
        val maxPillWidth = 220f
        val text = TripReportPdfTheme.fitText(periodLabel, maxPillWidth - 26f, paints.periodPillText)
        val textWidth = paints.periodPillText.measureText(text)
        val pillWidth = (textWidth + 26f).coerceAtMost(maxPillWidth)
        val rightX = TripReportPdfMetrics.RIGHT_EDGE_PT
        val pillRect = RectF(
            rightX - pillWidth,
            centerY - PERIOD_PILL_HEIGHT_PT / 2f,
            rightX,
            centerY + PERIOD_PILL_HEIGHT_PT / 2f
        )
        canvas.drawRoundRect(
            pillRect,
            TripReportPdfMetrics.PILL_RADIUS_PT,
            TripReportPdfMetrics.PILL_RADIUS_PT,
            paints.periodPillBg
        )
        paints.accentFill.color = TripReportPdfPalette.HORIZON_TEAL
        canvas.drawCircle(pillRect.left + 10f, centerY, PERIOD_DOT_RADIUS_PT, paints.accentFill)
        canvas.drawText(text, pillRect.left + 17f, centerY + 3f, paints.periodPillText)
    }

    fun measureHeroGroupCardHeight(groupDescription: String, paints: TripReportPdfPaints): Float {
        val descLines = if (groupDescription.isBlank()) {
            0
        } else {
            TripReportPdfTheme.wrapText(
                text = groupDescription,
                maxWidth = TripReportPdfMetrics.CONTENT_WIDTH_PT - 32f,
                paint = paints.heroSubtitle,
                maxLines = MAX_HERO_DESC_LINES
            ).size
        }
        return TripReportPdfMetrics.HERO_CARD_BASE_HEIGHT_PT +
            (descLines * TripReportPdfMetrics.HERO_DESCRIPTION_LINE_PT)
    }

    @Suppress("LongParameterList")
    fun drawHeroGroupCard(
        canvas: Canvas,
        reportTitle: String,
        groupName: String,
        groupDescription: String,
        metaLine: String,
        paints: TripReportPdfPaints,
        topY: Float
    ): Float {
        val innerWidth = TripReportPdfMetrics.CONTENT_WIDTH_PT - 32f
        val descLines = if (groupDescription.isBlank()) {
            emptyList()
        } else {
            TripReportPdfTheme.wrapText(groupDescription, innerWidth, paints.heroSubtitle, MAX_HERO_DESC_LINES)
        }
        val cardHeight = TripReportPdfMetrics.HERO_CARD_BASE_HEIGHT_PT +
            (descLines.size * TripReportPdfMetrics.HERO_DESCRIPTION_LINE_PT)

        val cardRect = RectF(
            TripReportPdfMetrics.MARGIN_X_PT,
            topY,
            TripReportPdfMetrics.RIGHT_EDGE_PT,
            topY + cardHeight
        )
        canvas.drawRoundRect(
            cardRect,
            TripReportPdfMetrics.CARD_RADIUS_PT,
            TripReportPdfMetrics.CARD_RADIUS_PT,
            paints.cardWhite
        )
        canvas.drawRoundRect(
            cardRect,
            TripReportPdfMetrics.CARD_RADIUS_PT,
            TripReportPdfMetrics.CARD_RADIUS_PT,
            paints.ghostStroke
        )
        drawHeroTopAccentStripe(canvas, cardRect)

        val textX = cardRect.left + 16f
        var cursorY = topY + 20f
        canvas.drawText(
            TripReportPdfTheme.fitText(reportTitle.uppercase(Locale.getDefault()), innerWidth, paints.kicker),
            textX,
            cursorY,
            paints.kicker
        )
        cursorY += 22f
        canvas.drawText(
            TripReportPdfTheme.fitText(groupName, innerWidth, paints.heroTitle),
            textX,
            cursorY,
            paints.heroTitle
        )
        descLines.forEach { line ->
            cursorY += TripReportPdfMetrics.HERO_DESCRIPTION_LINE_PT
            canvas.drawText(line, textX, cursorY, paints.heroSubtitle)
        }
        cursorY += 15f
        canvas.drawText(
            TripReportPdfTheme.fitText(metaLine, innerWidth, paints.heroMeta),
            textX,
            cursorY,
            paints.heroMeta
        )

        return topY + cardHeight + 10f
    }

    private fun drawHeroTopAccentStripe(canvas: Canvas, cardRect: RectF) {
        val stripePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                cardRect.left,
                cardRect.top,
                cardRect.right,
                cardRect.top,
                TripReportPdfPalette.HORIZON_BLUE,
                TripReportPdfPalette.HORIZON_TEAL,
                Shader.TileMode.CLAMP
            )
        }
        canvas.save()
        val clipPath = Path().apply {
            addRoundRect(
                cardRect,
                TripReportPdfMetrics.CARD_RADIUS_PT,
                TripReportPdfMetrics.CARD_RADIUS_PT,
                Path.Direction.CW
            )
        }
        canvas.clipPath(clipPath)
        canvas.drawRect(
            cardRect.left,
            cardRect.top,
            cardRect.right,
            cardRect.top + HERO_STRIPE_HEIGHT_PT,
            stripePaint
        )
        canvas.restore()
    }

    fun measureSummaryGridHeight(metrics: List<TripReportMetricCard>): Float = when {
        metrics.isEmpty() -> 0f
        metrics.size <= 3 -> TripReportPdfMetrics.KPI_GRID_SINGLE_ROW_HEIGHT_PT
        else -> TripReportPdfMetrics.KPI_GRID_TWO_ROW_HEIGHT_PT
    }

    fun drawSummaryMetricsGrid(
        canvas: Canvas,
        metrics: List<TripReportMetricCard>,
        paints: TripReportPdfPaints,
        topY: Float
    ): Float {
        if (metrics.isEmpty()) return topY
        val firstRow = metrics.take(3)
        val secondRow = metrics.drop(3)

        drawKpiRow(canvas, firstRow, paints, topY)
        if (secondRow.isNotEmpty()) {
            drawKpiRow(canvas, secondRow, paints, topY + KPI_HEIGHT_PT + KPI_GAP_PT)
            return topY + TripReportPdfMetrics.KPI_GRID_TWO_ROW_HEIGHT_PT
        }
        return topY + TripReportPdfMetrics.KPI_GRID_SINGLE_ROW_HEIGHT_PT
    }

    private fun drawKpiRow(
        canvas: Canvas,
        row: List<TripReportMetricCard>,
        paints: TripReportPdfPaints,
        topY: Float
    ) {
        val count = row.size
        val totalGaps = KPI_GAP_PT * (count - 1)
        val cardWidth = (TripReportPdfMetrics.CONTENT_WIDTH_PT - totalGaps) / count.toFloat()

        row.forEachIndexed { index, metric ->
            val left = TripReportPdfMetrics.MARGIN_X_PT + index * (cardWidth + KPI_GAP_PT)
            val rect = RectF(left, topY, left + cardWidth, topY + KPI_HEIGHT_PT)
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

            val toneColor = TripReportPdfTheme.resolveToneColor(metric.tone)
            paints.accentFill.color = toneColor
            val barRect = RectF(left + 8f, topY + 11f, left + 8f + KPI_ACCENT_WIDTH_PT, topY + KPI_HEIGHT_PT - 11f)
            canvas.drawRoundRect(barRect, 2f, 2f, paints.accentFill)

            val textX = left + 18f
            val maxTextWidth = cardWidth - 26f
            canvas.drawText(
                TripReportPdfTheme.fitText(metric.label, maxTextWidth, paints.kpiLabel),
                textX,
                topY + 21f,
                paints.kpiLabel
            )
            paints.kpiValue.color = toneColor
            canvas.drawText(
                TripReportPdfTheme.fitText(metric.value, maxTextWidth, paints.kpiValue),
                textX,
                topY + 41f,
                paints.kpiValue
            )
        }
    }

    fun drawProUpsellCard(
        canvas: Canvas,
        callout: TripReportProUpsellCallout,
        paints: TripReportPdfPaints,
        topY: Float
    ): Float {
        val cardHeight = TripReportPdfMetrics.PRO_UPSELL_CARD_HEIGHT_PT
        val cardRect = RectF(
            TripReportPdfMetrics.MARGIN_X_PT,
            topY,
            TripReportPdfMetrics.RIGHT_EDGE_PT,
            topY + cardHeight
        )
        canvas.drawRoundRect(
            cardRect,
            TripReportPdfMetrics.CARD_RADIUS_PT,
            TripReportPdfMetrics.CARD_RADIUS_PT,
            paints.upsellBg
        )

        val accentBar = RectF(cardRect.left + 10f, cardRect.top + 12f, cardRect.left + 14f, cardRect.bottom - 12f)
        val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                accentBar.left,
                accentBar.top,
                accentBar.left,
                accentBar.bottom,
                TripReportPdfPalette.HORIZON_BLUE,
                TripReportPdfPalette.HORIZON_TEAL,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(accentBar, 2f, 2f, barPaint)

        val contentX = cardRect.left + 24f
        val maxWidth = cardRect.width() - 38f

        val badgeWidth = paints.badgeText.measureText(callout.badge) + 14f
        val badgeRect = RectF(contentX, topY + 12f, contentX + badgeWidth, topY + 26f)
        paints.badgeBg.color = TripReportPdfPalette.HORIZON_BLUE
        paints.badgeText.color = TripReportPdfPalette.CARD_PROMINENT
        canvas.drawRoundRect(
            badgeRect,
            TripReportPdfMetrics.PILL_RADIUS_PT,
            TripReportPdfMetrics.PILL_RADIUS_PT,
            paints.badgeBg
        )
        canvas.drawText(callout.badge, badgeRect.left + 7f, topY + 21.5f, paints.badgeText)

        canvas.drawText(
            TripReportPdfTheme.fitText(callout.title, maxWidth - badgeWidth - 10f, paints.upsellTitle),
            badgeRect.right + 8f,
            topY + 23f,
            paints.upsellTitle
        )

        val bodyLines = TripReportPdfTheme.wrapText(callout.body, maxWidth, paints.upsellBody, MAX_UPSELL_BODY_LINES)
        var bodyY = topY + 42f
        bodyLines.forEach { line ->
            canvas.drawText(line, contentX, bodyY, paints.upsellBody)
            bodyY += 13.5f
        }

        return topY + cardHeight + 10f
    }

    fun drawBrandFooterCard(
        canvas: Canvas,
        brandFooter: TripReportBrandFooter,
        paints: TripReportPdfPaints,
        topY: Float
    ): Float {
        val cardHeight = TripReportPdfMetrics.BRAND_FOOTER_CARD_HEIGHT_PT
        val cardRect = RectF(
            TripReportPdfMetrics.MARGIN_X_PT,
            topY,
            TripReportPdfMetrics.RIGHT_EDGE_PT,
            topY + cardHeight
        )
        canvas.drawRoundRect(
            cardRect,
            TripReportPdfMetrics.CARD_RADIUS_PT,
            TripReportPdfMetrics.CARD_RADIUS_PT,
            paints.cardWhite
        )
        canvas.drawRoundRect(
            cardRect,
            TripReportPdfMetrics.CARD_RADIUS_PT,
            TripReportPdfMetrics.CARD_RADIUS_PT,
            paints.ghostStroke
        )

        val qrLeft = cardRect.right - TripReportPdfGraphics.QR_BOX_SIZE_PT - 16f
        val qrTop = topY + 11f
        val leftX = cardRect.left + 16f
        val textMaxWidth = (qrLeft - leftX - 18f).coerceAtLeast(120f)

        drawFooterBrandAndCopy(
            canvas = canvas,
            brandFooter = brandFooter,
            leftX = leftX,
            topY = topY,
            cardHeight = cardHeight,
            textMaxWidth = textMaxWidth,
            paints = paints
        )

        TripReportPdfGraphics.drawQrCodeTile(
            canvas = canvas,
            url = brandFooter.qrUrl,
            caption = brandFooter.qrCaption,
            left = qrLeft,
            top = qrTop,
            paints = paints
        )

        return topY + cardHeight
    }

    @Suppress("LongParameterList")
    private fun drawFooterBrandAndCopy(
        canvas: Canvas,
        brandFooter: TripReportBrandFooter,
        leftX: Float,
        topY: Float,
        cardHeight: Float,
        textMaxWidth: Float,
        paints: TripReportPdfPaints
    ) {
        TripReportPdfGraphics.drawBrandLogoVector(
            canvas = canvas,
            left = leftX,
            top = topY + 14f,
            size = FOOTER_LOGO_SIZE_PT
        )
        val wordX = leftX + FOOTER_LOGO_SIZE_PT + 8f
        val splitWidth = paints.brandSplit.measureText("Split")
        canvas.drawText("Split", wordX, topY + 29f, paints.brandSplit)
        canvas.drawText("Trip", wordX + splitWidth, topY + 29f, paints.brandTrip)

        canvas.drawText(
            TripReportPdfTheme.fitText(brandFooter.ctaHeadline, textMaxWidth, paints.footerCtaTitle),
            leftX,
            topY + 51f,
            paints.footerCtaTitle
        )
        val subLines = TripReportPdfTheme.wrapText(
            brandFooter.ctaSubtitle,
            textMaxWidth,
            paints.footerCtaSubtitle,
            MAX_CTA_SUBTITLE_LINES
        )
        var subY = topY + 65f
        subLines.forEach { line ->
            canvas.drawText(line, leftX, subY, paints.footerCtaSubtitle)
            subY += 12f
        }
        canvas.drawText(
            TripReportPdfTheme.fitText(brandFooter.websiteDisplay, textMaxWidth, paints.footerCtaUrl),
            leftX,
            topY + cardHeight - 11f,
            paints.footerCtaUrl
        )
    }

    fun drawRunningPageFooter(
        canvas: Canvas,
        websiteDisplay: String,
        pageFooterText: String,
        paints: TripReportPdfPaints
    ) {
        val footerY = TripReportPdfMetrics.A4_HEIGHT_PT - 16f
        val ruleY = footerY - 12f
        canvas.drawLine(
            TripReportPdfMetrics.MARGIN_X_PT,
            ruleY,
            TripReportPdfMetrics.RIGHT_EDGE_PT,
            ruleY,
            paints.ghostStroke
        )
        canvas.drawText("SplitTrip  ·  $websiteDisplay", TripReportPdfMetrics.MARGIN_X_PT, footerY, paints.footerBrand)
        val pageWidth = paints.footerPage.measureText(pageFooterText)
        canvas.drawText(
            pageFooterText,
            TripReportPdfMetrics.RIGHT_EDGE_PT - pageWidth,
            footerY,
            paints.footerPage
        )
    }
}
