@file:Suppress("MagicNumber")

package es.pedrazamiguez.splittrip.data.local.service.export

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import es.pedrazamiguez.splittrip.domain.model.report.TripReportTone

internal object TripReportPdfMetrics {
    const val A4_WIDTH_PT = 595
    const val A4_HEIGHT_PT = 842
    const val MARGIN_X_PT = 32f
    const val MARGIN_TOP_PT = 30f
    const val FOOTER_ZONE_HEIGHT_PT = 34f
    const val RIGHT_EDGE_PT = A4_WIDTH_PT - MARGIN_X_PT
    const val CONTENT_WIDTH_PT = RIGHT_EDGE_PT - MARGIN_X_PT
    const val MAX_CONTENT_Y_PT = A4_HEIGHT_PT - FOOTER_ZONE_HEIGHT_PT

    const val CARD_RADIUS_PT = 12f
    const val SMALL_CARD_RADIUS_PT = 10f
    const val PILL_RADIUS_PT = 999f
    const val TABLE_INSET_PT = 10f
    const val TABLE_INNER_WIDTH_PT = CONTENT_WIDTH_PT - (TABLE_INSET_PT * 2f)
    const val CELL_PAD_PT = 6f
    const val MIN_COLUMN_WIDTH_PT = 22f
    const val ELLIPSIS = "..."

    const val BRAND_HEADER_HEIGHT_PT = 42f
    const val HERO_CARD_BASE_HEIGHT_PT = 66f
    const val HERO_DESCRIPTION_LINE_PT = 14f
    const val KPI_GRID_SINGLE_ROW_HEIGHT_PT = 62f
    const val KPI_GRID_TWO_ROW_HEIGHT_PT = 122f
    const val SECTION_HEADING_HEIGHT_PT = 26f
    const val TABLE_HEADER_HEIGHT_PT = 24f
    const val TABLE_ROW_HEIGHT_PT = 22f
    const val EMPTY_SECTION_HEIGHT_PT = 34f
    const val PRO_UPSELL_CARD_HEIGHT_PT = 92f
    const val BRAND_FOOTER_CARD_HEIGHT_PT = 102f
    const val SPACER_HEIGHT_PT = 12f
}

internal object TripReportPdfPalette {
    val PAGE_SURFACE = Color.parseColor("#F9F9FF")
    val CARD_PROMINENT = Color.parseColor("#FFFFFF")
    val CARD_INSET = Color.parseColor("#F2F3FB")
    val ROW_ALT = Color.parseColor("#F7F8FD")
    val GHOST_BORDER = Color.parseColor("#E1E4F0")

    val HORIZON_BLUE = Color.parseColor("#00478D")
    val HORIZON_BLUE_LIGHT = Color.parseColor("#005EB8")
    val HORIZON_TEAL = Color.parseColor("#006874")
    val HORIZON_AMBER = Color.parseColor("#7C5800")

    val INK_PRIMARY = Color.parseColor("#191C21")
    val INK_SECONDARY = Color.parseColor("#41484F")
    val INK_MUTED = Color.parseColor("#6B7280")

    val SUCCESS = Color.parseColor("#1D7A5F")
    val SUCCESS_CONTAINER = Color.parseColor("#E3F5EE")
    val ON_SUCCESS_CONTAINER = Color.parseColor("#115743")

    val ERROR = Color.parseColor("#BA1A1A")
    val ERROR_CONTAINER = Color.parseColor("#FFDAD6")
    val ON_ERROR_CONTAINER = Color.parseColor("#7A0D0E")

    val WARNING_CONTAINER = Color.parseColor("#FFF0CC")
    val ON_WARNING_CONTAINER = Color.parseColor("#6B4B00")

    val INFO_CONTAINER = Color.parseColor("#D6E6FA")
    val ON_INFO_CONTAINER = Color.parseColor("#00315A")

    val UPSELL_BG = Color.parseColor("#EEF4FD")
    val QR_DARK = Color.parseColor("#00315A")
}

internal data class TripReportPdfTypefaces(
    val displayBold: Typeface,
    val bodyRegular: Typeface,
    val bodyMedium: Typeface,
    val bodySemiBold: Typeface,
    val bodyBold: Typeface
) {
    companion object {
        fun load(context: Context?): TripReportPdfTypefaces {
            val defaultRegular = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            val defaultBold = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            if (context == null) {
                return TripReportPdfTypefaces(
                    displayBold = defaultBold,
                    bodyRegular = defaultRegular,
                    bodyMedium = defaultRegular,
                    bodySemiBold = defaultBold,
                    bodyBold = defaultBold
                )
            }
            val jakartaBold = loadFontByName(context, "plus_jakarta_sans_bold") ?: defaultBold
            val manropeRegular = loadFontByName(context, "manrope_regular") ?: defaultRegular
            val manropeMedium = loadFontByName(context, "manrope_medium") ?: manropeRegular
            val manropeSemiBold = loadFontByName(context, "manrope_semibold") ?: defaultBold
            val manropeBold = loadFontByName(context, "manrope_bold") ?: defaultBold
            return TripReportPdfTypefaces(
                displayBold = jakartaBold,
                bodyRegular = manropeRegular,
                bodyMedium = manropeMedium,
                bodySemiBold = manropeSemiBold,
                bodyBold = manropeBold
            )
        }

        private fun loadFontByName(context: Context, fontName: String): Typeface? = runCatching {
            val resId = context.resources.getIdentifier(fontName, "font", context.packageName)
            if (resId != 0) ResourcesCompat.getFont(context, resId) else null
        }.getOrNull()
    }
}

internal data class TripReportPdfPaints(
    val typefaces: TripReportPdfTypefaces,
    val pageBg: Paint,
    val cardWhite: Paint,
    val cardInset: Paint,
    val rowAlt: Paint,
    val ghostStroke: Paint,
    val brandSplit: Paint,
    val brandTrip: Paint,
    val brandTagline: Paint,
    val periodPillBg: Paint,
    val periodPillText: Paint,
    val kicker: Paint,
    val heroTitle: Paint,
    val heroSubtitle: Paint,
    val heroMeta: Paint,
    val kpiLabel: Paint,
    val kpiValue: Paint,
    val sectionTitle: Paint,
    val tableHeader: Paint,
    val tableCell: Paint,
    val badgeBg: Paint,
    val badgeText: Paint,
    val upsellBg: Paint,
    val upsellTitle: Paint,
    val upsellBody: Paint,
    val footerBrand: Paint,
    val footerPage: Paint,
    val footerCtaTitle: Paint,
    val footerCtaSubtitle: Paint,
    val footerCtaUrl: Paint,
    val qrCaption: Paint,
    val qrDark: Paint,
    val accentFill: Paint
)

internal data class PdfBadgeColors(
    val backgroundColor: Int,
    val textColor: Int
)

private data class WrappedLineChunk(
    val line: String,
    val nextIndex: Int
)

internal object TripReportPdfTheme {

    // Manrope maps OpenType ligatures (tt -> U+E009, ff -> U+E007, ffl -> U+E008) to Private Use
    // Area codepoints that Skia writes verbatim into PDF text streams, breaking URL auto-detection
    // ("splittrip.eu" -> "rip.eu") as well as PDF text search and copy-paste.
    internal const val DISABLED_LIGATURES_FONT_FEATURES = "'liga' 0, 'clig' 0, 'dlig' 0, 'calt' 0"

    fun createPaints(context: Context?): TripReportPdfPaints {
        val fonts = TripReportPdfTypefaces.load(context)
        return TripReportPdfPaints(
            typefaces = fonts,
            pageBg = fillPaint(TripReportPdfPalette.PAGE_SURFACE),
            cardWhite = fillPaint(TripReportPdfPalette.CARD_PROMINENT),
            cardInset = fillPaint(TripReportPdfPalette.CARD_INSET),
            rowAlt = fillPaint(TripReportPdfPalette.ROW_ALT),
            ghostStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = TripReportPdfPalette.GHOST_BORDER
                style = Paint.Style.STROKE
                strokeWidth = 0.75f
            },
            brandSplit = textPaint(TripReportPdfPalette.INK_PRIMARY, 16f, fonts.displayBold),
            brandTrip = textPaint(TripReportPdfPalette.HORIZON_BLUE, 16f, fonts.displayBold),
            brandTagline = textPaint(TripReportPdfPalette.INK_SECONDARY, 7.5f, fonts.bodyMedium),
            periodPillBg = fillPaint(TripReportPdfPalette.INFO_CONTAINER),
            periodPillText = textPaint(TripReportPdfPalette.ON_INFO_CONTAINER, 8f, fonts.bodySemiBold),
            kicker = textPaint(TripReportPdfPalette.HORIZON_TEAL, 7.5f, fonts.bodyBold),
            heroTitle = textPaint(TripReportPdfPalette.INK_PRIMARY, 19f, fonts.displayBold),
            heroSubtitle = textPaint(TripReportPdfPalette.INK_SECONDARY, 9.5f, fonts.bodyRegular),
            heroMeta = textPaint(TripReportPdfPalette.INK_MUTED, 8f, fonts.bodyMedium),
            kpiLabel = textPaint(TripReportPdfPalette.INK_SECONDARY, 7.5f, fonts.bodySemiBold),
            kpiValue = textPaint(TripReportPdfPalette.INK_PRIMARY, 13.5f, fonts.displayBold),
            sectionTitle = textPaint(TripReportPdfPalette.INK_PRIMARY, 11.5f, fonts.displayBold),
            tableHeader = textPaint(TripReportPdfPalette.INK_SECONDARY, 7.5f, fonts.bodyBold),
            tableCell = textPaint(TripReportPdfPalette.INK_PRIMARY, 8.2f, fonts.bodyRegular),
            badgeBg = fillPaint(TripReportPdfPalette.INFO_CONTAINER),
            badgeText = textPaint(TripReportPdfPalette.ON_INFO_CONTAINER, 7.2f, fonts.bodyBold),
            upsellBg = fillPaint(TripReportPdfPalette.UPSELL_BG),
            upsellTitle = textPaint(TripReportPdfPalette.HORIZON_BLUE, 11f, fonts.displayBold),
            upsellBody = textPaint(TripReportPdfPalette.INK_SECONDARY, 8.5f, fonts.bodyRegular),
            footerBrand = textPaint(TripReportPdfPalette.HORIZON_BLUE, 8f, fonts.bodySemiBold),
            footerPage = textPaint(TripReportPdfPalette.INK_MUTED, 8f, fonts.bodyMedium),
            footerCtaTitle = textPaint(TripReportPdfPalette.INK_PRIMARY, 11f, fonts.displayBold),
            footerCtaSubtitle = textPaint(TripReportPdfPalette.INK_SECONDARY, 8.5f, fonts.bodyRegular),
            footerCtaUrl = textPaint(TripReportPdfPalette.HORIZON_BLUE, 9.5f, fonts.bodyBold),
            qrCaption = textPaint(TripReportPdfPalette.INK_MUTED, 7f, fonts.bodySemiBold),
            qrDark = fillPaint(TripReportPdfPalette.QR_DARK),
            accentFill = fillPaint(TripReportPdfPalette.HORIZON_BLUE)
        )
    }

    fun resolveToneColor(tone: TripReportTone): Int = when (tone) {
        TripReportTone.DEFAULT -> TripReportPdfPalette.INK_PRIMARY
        TripReportTone.MUTED -> TripReportPdfPalette.INK_SECONDARY
        TripReportTone.PRIMARY -> TripReportPdfPalette.HORIZON_BLUE
        TripReportTone.POSITIVE -> TripReportPdfPalette.SUCCESS
        TripReportTone.NEGATIVE -> TripReportPdfPalette.ERROR
        TripReportTone.TEAL -> TripReportPdfPalette.HORIZON_TEAL
        TripReportTone.AMBER -> TripReportPdfPalette.HORIZON_AMBER
        TripReportTone.BADGE_SUCCESS -> TripReportPdfPalette.ON_SUCCESS_CONTAINER
        TripReportTone.BADGE_WARNING -> TripReportPdfPalette.ON_WARNING_CONTAINER
        TripReportTone.BADGE_ERROR -> TripReportPdfPalette.ON_ERROR_CONTAINER
        TripReportTone.BADGE_INFO -> TripReportPdfPalette.ON_INFO_CONTAINER
    }

    fun resolveBadgeColors(tone: TripReportTone): PdfBadgeColors? = when (tone) {
        TripReportTone.BADGE_SUCCESS -> PdfBadgeColors(
            backgroundColor = TripReportPdfPalette.SUCCESS_CONTAINER,
            textColor = TripReportPdfPalette.ON_SUCCESS_CONTAINER
        )
        TripReportTone.BADGE_WARNING -> PdfBadgeColors(
            backgroundColor = TripReportPdfPalette.WARNING_CONTAINER,
            textColor = TripReportPdfPalette.ON_WARNING_CONTAINER
        )
        TripReportTone.BADGE_ERROR -> PdfBadgeColors(
            backgroundColor = TripReportPdfPalette.ERROR_CONTAINER,
            textColor = TripReportPdfPalette.ON_ERROR_CONTAINER
        )
        TripReportTone.BADGE_INFO -> PdfBadgeColors(
            backgroundColor = TripReportPdfPalette.INFO_CONTAINER,
            textColor = TripReportPdfPalette.ON_INFO_CONTAINER
        )
        else -> null
    }

    fun resolveToneTypeface(tone: TripReportTone, fonts: TripReportPdfTypefaces): Typeface = when (tone) {
        TripReportTone.PRIMARY,
        TripReportTone.POSITIVE,
        TripReportTone.NEGATIVE,
        TripReportTone.AMBER -> fonts.bodyBold
        TripReportTone.TEAL -> fonts.bodySemiBold
        TripReportTone.DEFAULT,
        TripReportTone.MUTED,
        TripReportTone.BADGE_SUCCESS,
        TripReportTone.BADGE_WARNING,
        TripReportTone.BADGE_ERROR,
        TripReportTone.BADGE_INFO -> fonts.bodyRegular
    }

    fun fitText(text: String, maxWidth: Float, paint: Paint): String {
        if (text.isEmpty() || maxWidth <= 0f || paint.measureText(text) <= maxWidth) return text
        val ellipsisWidth = paint.measureText(TripReportPdfMetrics.ELLIPSIS)
        val available = (maxWidth - ellipsisWidth).coerceAtLeast(0f)
        val count = paint.breakText(text, true, available, null)
        return text.take(count) + TripReportPdfMetrics.ELLIPSIS
    }

    fun wrapText(text: String, maxWidth: Float, paint: Paint, maxLines: Int): List<String> {
        if (text.isBlank() || maxWidth <= 0f || maxLines <= 0) return emptyList()
        val words = text.trim().split(Regex("\\s+"))
        val lines = mutableListOf<String>()
        var startIndex = 0
        while (startIndex < words.size && lines.size < maxLines) {
            val isLastLine = lines.size == maxLines - 1
            if (isLastLine) {
                val remainder = words.subList(startIndex, words.size).joinToString(" ")
                lines.add(fitText(remainder, maxWidth, paint))
                break
            }
            val chunk = consumeSingleLine(words, startIndex, maxWidth, paint)
            lines.add(chunk.line)
            startIndex = chunk.nextIndex
        }
        return lines
    }

    private fun consumeSingleLine(
        words: List<String>,
        startIndex: Int,
        maxWidth: Float,
        paint: Paint
    ): WrappedLineChunk {
        var current = words[startIndex]
        var nextIndex = startIndex + 1
        while (nextIndex < words.size) {
            val candidate = "$current ${words[nextIndex]}"
            if (paint.measureText(candidate) > maxWidth) break
            current = candidate
            nextIndex++
        }
        return WrappedLineChunk(line = fitText(current, maxWidth, paint), nextIndex = nextIndex)
    }

    private fun fillPaint(colorInt: Int): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colorInt
        style = Paint.Style.FILL
    }

    private fun textPaint(colorInt: Int, sizePt: Float, tf: Typeface): Paint =
        Paint(
            Paint.ANTI_ALIAS_FLAG or
                Paint.LINEAR_TEXT_FLAG or
                Paint.SUBPIXEL_TEXT_FLAG
        ).apply {
            color = colorInt
            textSize = sizePt
            typeface = tf
            hinting = Paint.HINTING_OFF
            fontFeatureSettings = DISABLED_LIGATURES_FONT_FEATURES
        }
}
