@file:Suppress("MagicNumber")

package es.pedrazamiguez.splittrip.data.local.service.export

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter

internal object TripReportPdfGraphics {

    private const val LOGO_VIEWPORT = 2048f
    private const val LOGO_STROKE_WIDTH = 148f
    const val QR_BOX_SIZE_PT = 70f
    private const val QR_PADDING_PT = 5f
    private const val QR_ENCODE_MATRIX_SIZE = 33

    fun drawQrCodeTile(
        canvas: Canvas,
        url: String,
        caption: String,
        left: Float,
        top: Float,
        paints: TripReportPdfPaints
    ) {
        val tileRect = RectF(left, top, left + QR_BOX_SIZE_PT, top + QR_BOX_SIZE_PT)
        canvas.drawRoundRect(tileRect, 8f, 8f, paints.cardInset)

        val matrix = runCatching {
            QRCodeWriter().encode(
                url,
                BarcodeFormat.QR_CODE,
                QR_ENCODE_MATRIX_SIZE,
                QR_ENCODE_MATRIX_SIZE,
                mapOf(EncodeHintType.MARGIN to 1)
            )
        }.getOrNull()

        if (matrix != null) {
            drawQrMatrix(canvas, matrix, left, top, paints.qrDark)
        }

        val captionText = TripReportPdfTheme.fitText(caption, QR_BOX_SIZE_PT + 16f, paints.qrCaption)
        val captionWidth = paints.qrCaption.measureText(captionText)
        canvas.drawText(
            captionText,
            left + (QR_BOX_SIZE_PT - captionWidth) / 2f,
            top + QR_BOX_SIZE_PT + 11f,
            paints.qrCaption
        )
    }

    private fun drawQrMatrix(
        canvas: Canvas,
        matrix: BitMatrix,
        left: Float,
        top: Float,
        darkPaint: Paint
    ) {
        val innerSize = QR_BOX_SIZE_PT - (QR_PADDING_PT * 2f)
        val cellWidth = innerSize / matrix.width.toFloat()
        val cellHeight = innerSize / matrix.height.toFloat()
        val startX = left + QR_PADDING_PT
        val startY = top + QR_PADDING_PT
        for (y in 0 until matrix.height) {
            for (x in 0 until matrix.width) {
                if (matrix[x, y]) {
                    val cellLeft = startX + x * cellWidth
                    val cellTop = startY + y * cellHeight
                    canvas.drawRect(
                        cellLeft,
                        cellTop,
                        cellLeft + cellWidth + 0.2f,
                        cellTop + cellHeight + 0.2f,
                        darkPaint
                    )
                }
            }
        }
    }

    fun drawBrandLogoVector(canvas: Canvas, left: Float, top: Float, size: Float) {
        val scale = size / LOGO_VIEWPORT
        canvas.save()
        canvas.translate(left, top)
        canvas.scale(scale, scale)

        val upperPath = Path().apply {
            moveTo(1105f, 2101f)
            cubicTo(1427.5f, 1833.5f, 1503f, 1586f, 1427.5f, 1384.5f)
            cubicTo(1194f, 892.5f, 530f, 1037.5f, 606.5f, 501.5f)
            cubicTo(671f, 200f, 1113.5f, 180.1f, 1224.5f, 200f)
            cubicTo(1551.5f, 258.5f, 1802.5f, 482.5f, 1687f, 625f)
            cubicTo(1571f, 723.5f, 1481f, 534.2f, 1308f, 465f)
            cubicTo(1152f, 402.6f, 942.5f, 390f, 885.5f, 538.5f)
            cubicTo(786.5f, 855f, 1699.5f, 846f, 1727.5f, 1359f)
            cubicTo(1727.5f, 1558f, 1621.5f, 1645.5f, 1610f, 1655f)
        }
        val upperPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = LOGO_STROKE_WIDTH
            strokeCap = Paint.Cap.ROUND
            shader = LinearGradient(
                1288.5f,
                476f,
                1492.5f,
                2014.5f,
                TripReportPdfPalette.HORIZON_TEAL,
                TripReportPdfPalette.HORIZON_BLUE,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawPath(upperPath, upperPaint)

        val lowerPath = Path().apply {
            moveTo(1145f, 1828.5f)
            cubicTo(1109f, 1841f, 933f, 1878.5f, 699f, 1791.5f)
            cubicTo(542f, 1725.5f, 248f, 1548f, 410.5f, 1412.5f)
            cubicTo(548f, 1329f, 610f, 1517.2f, 816f, 1601f)
            cubicTo(1022f, 1684.8f, 1164.5f, 1593f, 1164.5f, 1479.5f)
            cubicTo(1164.5f, 1230.5f, 380.5f, 1139f, 365f, 732.5f)
            cubicTo(358.6f, 565.5f, 453f, 455f, 472f, 424f)
        }
        val lowerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = LOGO_STROKE_WIDTH
            strokeCap = Paint.Cap.ROUND
            shader = LinearGradient(
                363f,
                573.5f,
                1076f,
                1847f,
                TripReportPdfPalette.HORIZON_BLUE,
                TripReportPdfPalette.HORIZON_TEAL,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawPath(lowerPath, lowerPaint)

        canvas.restore()
    }
}
