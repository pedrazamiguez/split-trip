package es.pedrazamiguez.splittrip.data.local.service.export

import android.graphics.RectF
import java.util.Locale
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TripReportPdfLinkAnnotatorTest {

    @Test
    fun `appendLinkAnnotations returns original bytes unchanged when links map is empty`() {
        val original = buildNestedPagesPdf()
        val result = TripReportPdfLinkAnnotator.appendLinkAnnotations(
            pdfBytes = original,
            linksByPage = mapOf(0 to emptyList()),
            pageHeightPt = 842f
        )

        assertArrayEquals(original, result)
    }

    @Test
    fun `appendLinkAnnotations returns original bytes unchanged when input lacks classic xref trailer`() {
        val stubBytes = "%PDF-1.4\n".toByteArray(Charsets.ISO_8859_1)
        val result = TripReportPdfLinkAnnotator.appendLinkAnnotations(
            pdfBytes = stubBytes,
            linksByPage = mapOf(0 to listOf(PdfLinkAnnotation(RectF(10f, 20f, 100f, 35f), "https://splittrip.eu"))),
            pageHeightPt = 842f
        )

        assertArrayEquals(stubBytes, result)
    }

    @Test
    fun `appendLinkAnnotations resolves nested Pages tree and appends incremental xref and Annots per page`() {
        val original = buildNestedPagesPdf()
        val originalText = original.toString(Charsets.ISO_8859_1)
        val originalStartXref = Regex("""startxref\s+(\d+)""").find(originalText)!!.groupValues[1].toInt()
        val originalSize = Regex("""/Size\s+(\d+)""").find(originalText)!!.groupValues[1].toInt()

        val page0Link = PdfLinkAnnotation(RectF(32f, 800f, 150f, 820f), "https://splittrip.eu")
        val page2Link = PdfLinkAnnotation(RectF(48f, 750f, 180f, 770f), "https://splittrip.eu")

        val result = TripReportPdfLinkAnnotator.appendLinkAnnotations(
            pdfBytes = original,
            linksByPage = mapOf(
                0 to listOf(page0Link),
                2 to listOf(page2Link)
            ),
            pageHeightPt = 842f
        )

        // Original byte prefix preserved byte-for-byte
        assertArrayEquals(original, result.copyOfRange(0, original.size))

        val resultText = result.toString(Charsets.ISO_8859_1)
        val tailText = resultText.substring(original.size)

        // /Prev points to original startxref, /Size equals originalSize + 2 new annotations
        assertTrue(tailText.contains("/Prev $originalStartXref"))
        assertTrue(tailText.contains("/Size ${originalSize + 2}"))

        // Incremental xref contains valid 20-byte entries whose offsets resolve directly to updated objects
        val newStartXref = Regex("""startxref\s+(\d+)""").find(tailText)!!.groupValues[1].toInt()
        assertTrue(newStartXref > original.size)
        val xrefSection = resultText.substring(newStartXref)
        val entryMatches = Regex("""(\d{10}) 00000 n \n""").findAll(xrefSection).toList()
        assertEquals(4, entryMatches.size) // 2 updated Page objects + 2 new Annot objects
        entryMatches.forEach { match ->
            assertEquals(20, match.value.length)
            val offset = match.groupValues[1].toInt()
            val objHeader = resultText.substring(offset, (offset + 20).coerceAtMost(resultText.length))
            assertTrue(objHeader.matches(Regex("""^\d+ 0 obj[\s\S]*""")))
        }

        // Exact Canvas-to-PDF Y inversion: lly = 842 - bottom, ury = 842 - top
        // Page 0: top=800, bottom=820 -> lly=22.00, ury=42.00
        assertTrue(tailText.contains("/Rect [32.00 22.00 150.00 42.00]"))
        // Page 2: top=750, bottom=770 -> lly=72.00, ury=92.00
        assertTrue(tailText.contains("/Rect [48.00 72.00 180.00 92.00]"))

        // Page 1 (obj 5) was not annotated so it must not appear in updated tail
        assertFalse(tailText.contains("5 0 obj"))
    }

    @Test
    fun `appendLinkAnnotations escapes parentheses and backslashes in URI literal strings`() {
        val original = buildNestedPagesPdf()
        val uriWithSpecialChars = "https://splittrip.eu/path(1)\\test"

        val result = TripReportPdfLinkAnnotator.appendLinkAnnotations(
            pdfBytes = original,
            linksByPage = mapOf(0 to listOf(PdfLinkAnnotation(RectF(10f, 10f, 50f, 20f), uriWithSpecialChars))),
            pageHeightPt = 842f
        )

        val resultText = result.toString(Charsets.ISO_8859_1)
        assertTrue(resultText.contains("""/URI (https://splittrip.eu/path\(1\)\\test)"""))
    }

    @Test
    fun `appendLinkAnnotations merges into existing inline Annots array on a Page dictionary`() {
        val original = buildPdfWithExistingInlineAnnots()
        val result = TripReportPdfLinkAnnotator.appendLinkAnnotations(
            pdfBytes = original,
            linksByPage = mapOf(0 to listOf(PdfLinkAnnotation(RectF(10f, 10f, 80f, 30f), "https://splittrip.eu"))),
            pageHeightPt = 842f
        )

        val tailText = result.toString(Charsets.ISO_8859_1).substring(original.size)
        assertTrue(tailText.contains("/Annots [99 0 R 4 0 R]"))
    }

    private fun buildNestedPagesPdf(): ByteArray {
        val sb = StringBuilder("%PDF-1.4\n")
        val offsets = mutableMapOf<Int, Int>()

        // 1: Catalog -> 2: Root Pages -> [3: Page 0 (with nested Resources + stream), 4: Nested Pages -> [5: Page 1, 6: Page 2]]
        offsets[1] = sb.length
        sb.append("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n")

        offsets[2] = sb.length
        sb.append("2 0 obj\n<< /Type /Pages /Count 3 /Kids [3 0 R 4 0 R] >>\nendobj\n")

        offsets[3] = sb.length
        sb.append(
            "3 0 obj\n<< /Type /Page /Parent 2 0 R /Resources << /Font << /F1 7 0 R >> >> " +
                "/MediaBox [0 0 595 842] >>\nstream\nBT /F1 12 Tf (Hello >>) Tj ET\nendstream\nendobj\n"
        )

        offsets[4] = sb.length
        sb.append("4 0 obj\n<< /Type /Pages /Parent 2 0 R /Count 2 /Kids [5 0 R 6 0 R] >>\nendobj\n")

        offsets[5] = sb.length
        sb.append("5 0 obj\n<< /Type /Page /Parent 4 0 R /MediaBox [0 0 595 842] >>\nendobj\n")

        offsets[6] = sb.length
        sb.append("6 0 obj\n<< /Type /Page /Parent 4 0 R /MediaBox [0 0 595 842] >>\nendobj\n")

        val totalObjects = 7
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

    private fun buildPdfWithExistingInlineAnnots(): ByteArray {
        val sb = StringBuilder("%PDF-1.4\n")
        val offsets = mutableMapOf<Int, Int>()

        offsets[1] = sb.length
        sb.append("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n")

        offsets[2] = sb.length
        sb.append("2 0 obj\n<< /Type /Pages /Count 1 /Kids [3 0 R] >>\nendobj\n")

        offsets[3] = sb.length
        sb.append("3 0 obj\n<< /Type /Page /Parent 2 0 R /Annots [99 0 R] /MediaBox [0 0 595 842] >>\nendobj\n")

        val totalObjects = 4
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
}
