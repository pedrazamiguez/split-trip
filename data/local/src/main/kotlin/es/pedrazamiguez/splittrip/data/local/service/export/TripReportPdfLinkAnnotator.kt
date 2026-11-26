package es.pedrazamiguez.splittrip.data.local.service.export

import android.graphics.RectF
import java.io.ByteArrayOutputStream
import java.util.Locale
import java.util.SortedMap

internal data class PdfLinkAnnotation(
    val rectCanvasPt: RectF,
    val uri: String
)

private data class XrefEntryOffset(
    val objId: Int,
    val offset: Int
)

private data class PdfStructure(
    val oldStartXref: Int,
    val originalSize: Int,
    val rootRef: String,
    val infoRef: String?,
    val objectOffsets: Map<Int, Int>
)

internal object TripReportPdfLinkAnnotator {

    private const val XREF_KEYWORD = "xref"
    private const val TRAILER_KEYWORD = "trailer"
    private const val STARTXREF_KEYWORD = "startxref"
    private const val DICT_CLOSE = ">>"
    private const val ESCAPE_CHAR = '\\'
    private const val STRING_OPEN = '('
    private const val STRING_CLOSE = ')'
    private const val NEWLINE_BYTE = '\n'.code
    private const val CR_BYTE = '\r'.code
    private const val LINK_BORDER = "/Border [0 0 0]"
    private const val DEFAULT_XREF_GEN = "00000"
    private const val IN_USE_FLAG = "n"
    private const val ESTIMATED_BYTES_PER_ANNOTATED_PAGE = 512

    private val OBJ_ID_FROM_REF_REGEX = Regex("""(\d+)\s+\d+\s+R""")
    private val INLINE_ANNOTS_REGEX = Regex("""(/Annots\s*\[)([^\]]*)(\])""", RegexOption.DOT_MATCHES_ALL)

    fun appendLinkAnnotations(
        pdfBytes: ByteArray,
        linksByPage: Map<Int, List<PdfLinkAnnotation>>,
        pageHeightPt: Float
    ): ByteArray {
        if (linksByPage.values.all { it.isEmpty() }) return pdfBytes

        val pdfText = pdfBytes.toString(Charsets.ISO_8859_1)
        val structure = PdfStructureReader.parsePdfStructure(pdfText) ?: return pdfBytes
        val pageObjIds = resolveOrderedPageIds(pdfText, structure) ?: return pdfBytes
        val annotatedPages = linksByPage.entries
            .filter { (pageIndex, links) -> links.isNotEmpty() && pageIndex in pageObjIds.indices }
            .sortedBy { it.key }
        if (annotatedPages.isEmpty()) return pdfBytes

        return buildIncrementalUpdate(
            pdfBytes = pdfBytes,
            pdfText = pdfText,
            structure = structure,
            pageObjIds = pageObjIds,
            annotatedPages = annotatedPages,
            pageHeightPt = pageHeightPt
        )
    }

    private fun resolveOrderedPageIds(pdfText: String, structure: PdfStructure): List<Int>? {
        val rootObjId = OBJ_ID_FROM_REF_REGEX.find(structure.rootRef)?.groupValues?.get(1)?.toIntOrNull()
        val rootDict = rootObjId?.let { PdfStructureReader.readObjectDictionary(pdfText, structure.objectOffsets, it) }
        val pagesObjId = rootDict?.let {
            Regex("""/Pages\s+(\d+)\s+\d+\s+R""").find(it)?.groupValues?.get(1)?.toIntOrNull()
        }
        return pagesObjId?.let { PdfStructureReader.collectPageObjectIds(pdfText, structure.objectOffsets, it) }
    }

    private fun buildIncrementalUpdate(
        pdfBytes: ByteArray,
        pdfText: String,
        structure: PdfStructure,
        pageObjIds: List<Int>,
        annotatedPages: List<Map.Entry<Int, List<PdfLinkAnnotation>>>,
        pageHeightPt: Float
    ): ByteArray {
        val out = ByteArrayOutputStream(pdfBytes.size + annotatedPages.size * ESTIMATED_BYTES_PER_ANNOTATED_PAGE)
        out.write(pdfBytes)
        ensureTrailingNewline(pdfBytes, out)

        var nextObjId = structure.originalSize
        val updatedOffsets = sortedMapOf<Int, Int>()

        for ((pageIndex, pageLinks) in annotatedPages) {
            val pageObjId = pageObjIds[pageIndex]
            val originalPageDict = PdfStructureReader.readObjectDictionary(pdfText, structure.objectOffsets, pageObjId)
                ?: continue
            val annotIds = emitLinkAnnotationObjects(
                out = out,
                links = pageLinks,
                pageHeightPt = pageHeightPt,
                startObjId = nextObjId,
                updatedOffsets = updatedOffsets
            )
            nextObjId += annotIds.size
            emitUpdatedPageObject(
                out = out,
                pageObjId = pageObjId,
                originalPageDict = originalPageDict,
                annotIds = annotIds,
                updatedOffsets = updatedOffsets
            )
        }

        if (updatedOffsets.isEmpty()) return pdfBytes

        writeIncrementalXrefAndTrailer(
            out = out,
            updatedOffsets = updatedOffsets,
            nextObjId = nextObjId,
            structure = structure
        )
        return out.toByteArray()
    }

    private fun emitLinkAnnotationObjects(
        out: ByteArrayOutputStream,
        links: List<PdfLinkAnnotation>,
        pageHeightPt: Float,
        startObjId: Int,
        updatedOffsets: SortedMap<Int, Int>
    ): List<Int> {
        val annotIds = ArrayList<Int>(links.size)
        var currentId = startObjId
        for (link in links) {
            val annotId = currentId++
            updatedOffsets[annotId] = out.size()
            val rect = link.rectCanvasPt
            val rectFormatted = String.format(
                Locale.US,
                "%.2f %.2f %.2f %.2f",
                rect.left,
                pageHeightPt - rect.bottom,
                rect.right,
                pageHeightPt - rect.top
            )
            val escapedUri = escapePdfLiteralString(link.uri)
            val objString = "$annotId 0 obj\n" +
                "<< /Type /Annot /Subtype /Link /Rect [$rectFormatted] $LINK_BORDER " +
                "/A << /Type /Action /S /URI /URI ($escapedUri) >> >>\n" +
                "endobj\n"
            out.write(objString.toByteArray(Charsets.ISO_8859_1))
            annotIds.add(annotId)
        }
        return annotIds
    }

    private fun emitUpdatedPageObject(
        out: ByteArrayOutputStream,
        pageObjId: Int,
        originalPageDict: String,
        annotIds: List<Int>,
        updatedOffsets: SortedMap<Int, Int>
    ) {
        val newRefs = annotIds.joinToString(" ") { "$it 0 R" }
        val updatedDict = mergeAnnotsIntoPageDict(originalPageDict, newRefs)
        updatedOffsets[pageObjId] = out.size()
        val objString = "$pageObjId 0 obj\n$updatedDict\nendobj\n"
        out.write(objString.toByteArray(Charsets.ISO_8859_1))
    }

    private fun mergeAnnotsIntoPageDict(pageDict: String, newRefs: String): String {
        val existingMatch = INLINE_ANNOTS_REGEX.find(pageDict)
        if (existingMatch != null) {
            val prefix = existingMatch.groupValues[1]
            val existingBody = existingMatch.groupValues[2].trim()
            val suffix = existingMatch.groupValues[3]
            val combined = if (existingBody.isEmpty()) newRefs else "$existingBody $newRefs"
            return pageDict.replaceRange(existingMatch.range, "$prefix$combined$suffix")
        }
        val closingIndex = pageDict.lastIndexOf(DICT_CLOSE)
        if (closingIndex < 0) return pageDict
        return buildString {
            append(pageDict, 0, closingIndex)
            append(" /Annots [")
            append(newRefs)
            append("] ")
            append(DICT_CLOSE)
        }
    }

    private fun writeIncrementalXrefAndTrailer(
        out: ByteArrayOutputStream,
        updatedOffsets: SortedMap<Int, Int>,
        nextObjId: Int,
        structure: PdfStructure
    ) {
        val newXrefOffset = out.size()
        out.write("$XREF_KEYWORD\n".toByteArray(Charsets.ISO_8859_1))

        val subsections = groupContiguousSubsections(updatedOffsets)
        for (subsection in subsections) {
            val startId = subsection.first().objId
            out.write("$startId ${subsection.size}\n".toByteArray(Charsets.ISO_8859_1))
            for (entry in subsection) {
                val line = String.format(Locale.US, "%010d $DEFAULT_XREF_GEN $IN_USE_FLAG \n", entry.offset)
                out.write(line.toByteArray(Charsets.ISO_8859_1))
            }
        }

        val infoPart = structure.infoRef?.let { " /Info $it" }.orEmpty()
        val trailer = "$TRAILER_KEYWORD\n" +
            "<< /Size $nextObjId /Root ${structure.rootRef}$infoPart /Prev ${structure.oldStartXref} >>\n" +
            "$STARTXREF_KEYWORD\n$newXrefOffset\n%%EOF\n"
        out.write(trailer.toByteArray(Charsets.ISO_8859_1))
    }

    private fun groupContiguousSubsections(
        updatedOffsets: SortedMap<Int, Int>
    ): List<List<XrefEntryOffset>> {
        val subsections = mutableListOf<MutableList<XrefEntryOffset>>()
        var currentGroup = mutableListOf<XrefEntryOffset>()
        var previousId: Int? = null
        for ((objId, offset) in updatedOffsets) {
            if (previousId != null && objId != previousId + 1) {
                subsections.add(currentGroup)
                currentGroup = mutableListOf()
            }
            currentGroup.add(XrefEntryOffset(objId = objId, offset = offset))
            previousId = objId
        }
        if (currentGroup.isNotEmpty()) {
            subsections.add(currentGroup)
        }
        return subsections
    }

    private fun ensureTrailingNewline(pdfBytes: ByteArray, out: ByteArrayOutputStream) {
        if (pdfBytes.isEmpty()) return
        val last = pdfBytes.last().toInt()
        if (last != NEWLINE_BYTE && last != CR_BYTE) {
            out.write(NEWLINE_BYTE)
        }
    }

    private fun escapePdfLiteralString(value: String): String = buildString(value.length) {
        for (ch in value) {
            when (ch) {
                ESCAPE_CHAR -> append("\\\\")
                STRING_OPEN -> append("\\(")
                STRING_CLOSE -> append("\\)")
                else -> append(ch)
            }
        }
    }
}

private object PdfStructureReader {

    private const val XREF_KEYWORD = "xref"
    private const val TRAILER_KEYWORD = "trailer"
    private const val STARTXREF_KEYWORD = "startxref"
    private const val IN_USE_FLAG = "n"
    private const val SUBSECTION_HEADER_TOKENS = 2
    private const val XREF_ENTRY_TOKENS = 3

    private val STARTXREF_REGEX = Regex("""startxref\s+(\d+)""")
    private val SIZE_REGEX = Regex("""/Size\s+(\d+)""")
    private val ROOT_REGEX = Regex("""/Root\s+(\d+\s+\d+\s+R)""")
    private val INFO_REGEX = Regex("""/Info\s+(\d+\s+\d+\s+R)""")
    private val PREV_REGEX = Regex("""/Prev\s+(\d+)""")
    private val OBJ_ID_FROM_REF_REGEX = Regex("""(\d+)\s+\d+\s+R""")
    private val KIDS_ARRAY_REGEX = Regex("""/Kids\s*\[([^\]]*)\]""", RegexOption.DOT_MATCHES_ALL)
    private val WHITESPACE_SPLIT_REGEX = Regex("""\s+""")

    fun parsePdfStructure(pdfText: String): PdfStructure? {
        val startXrefIndex = pdfText.lastIndexOf(STARTXREF_KEYWORD)
        if (startXrefIndex < 0) return null
        val oldStartXref = STARTXREF_REGEX.find(pdfText, startIndex = startXrefIndex)
            ?.groupValues?.get(1)?.toIntOrNull()
            ?.takeIf { it in 0 until pdfText.length }
        val trailerIdx = pdfText.lastIndexOf(TRAILER_KEYWORD, startIndex = startXrefIndex)
        val trailerDict = if (oldStartXref != null && trailerIdx >= 0) {
            PdfTextScanner.extractBalancedDictionary(pdfText, trailerIdx + TRAILER_KEYWORD.length)
        } else {
            null
        }
        return if (oldStartXref != null && trailerDict != null) {
            buildStructureFromTrailer(pdfText, oldStartXref, trailerDict)
        } else {
            null
        }
    }

    private fun buildStructureFromTrailer(
        pdfText: String,
        oldStartXref: Int,
        trailerDict: String
    ): PdfStructure? {
        val size = SIZE_REGEX.find(trailerDict)?.groupValues?.get(1)?.toIntOrNull() ?: return null
        val rootRef = ROOT_REGEX.find(trailerDict)?.groupValues?.get(1)?.trim() ?: return null
        val offsets = parseAllXrefSections(pdfText, oldStartXref) ?: return null
        return PdfStructure(
            oldStartXref = oldStartXref,
            originalSize = size,
            rootRef = rootRef,
            infoRef = INFO_REGEX.find(trailerDict)?.groupValues?.get(1)?.trim(),
            objectOffsets = offsets
        )
    }

    fun collectPageObjectIds(
        pdfText: String,
        objectOffsets: Map<Int, Int>,
        pagesObjId: Int
    ): List<Int> {
        val result = mutableListOf<Int>()
        val visited = mutableSetOf<Int>()
        collectPageIdsRecursive(pdfText, objectOffsets, pagesObjId, visited, result)
        return result
    }

    fun readObjectDictionary(
        pdfText: String,
        objectOffsets: Map<Int, Int>,
        objId: Int
    ): String? {
        val offset = objectOffsets[objId] ?: return null
        if (offset !in 0 until pdfText.length) return null
        return PdfTextScanner.extractBalancedDictionary(pdfText, offset)
    }

    private fun parseAllXrefSections(pdfText: String, initialOffset: Int): Map<Int, Int>? {
        val offsets = mutableMapOf<Int, Int>()
        val seenIds = mutableSetOf<Int>()
        val visitedOffsets = mutableSetOf<Int>()
        var currentOffset: Int? = initialOffset

        while (currentOffset != null && visitedOffsets.add(currentOffset)) {
            if (currentOffset !in 0 until pdfText.length) return null
            val headerPos = PdfTextScanner.skipWhitespace(pdfText, currentOffset)
            if (!pdfText.startsWith(XREF_KEYWORD, headerPos)) {
                return if (visitedOffsets.size == 1) null else offsets
            }
            val afterXref = parseXrefSubsections(pdfText, headerPos + XREF_KEYWORD.length, offsets, seenIds)
            val trailerDict = PdfTextScanner.extractBalancedDictionary(pdfText, afterXref)
            currentOffset = trailerDict?.let { dict ->
                PREV_REGEX.find(dict)?.groupValues?.get(1)?.toIntOrNull()
            }
        }
        return offsets
    }

    private fun parseXrefSubsections(
        pdfText: String,
        startPos: Int,
        offsets: MutableMap<Int, Int>,
        seenIds: MutableSet<Int>
    ): Int {
        var pos = PdfTextScanner.skipWhitespace(pdfText, startPos)
        var parsing = true
        while (parsing && pos < pdfText.length && !pdfText.startsWith(TRAILER_KEYWORD, pos)) {
            val lineEnd = PdfTextScanner.findLineEnd(pdfText, pos)
            val headerTokens = pdfText.substring(pos, lineEnd).trim().split(WHITESPACE_SPLIT_REGEX)
            pos = PdfTextScanner.skipWhitespace(pdfText, lineEnd)
            val startId = headerTokens.getOrNull(0)?.toIntOrNull()
            val count = headerTokens.getOrNull(1)?.toIntOrNull()
            if (headerTokens.size == SUBSECTION_HEADER_TOKENS && startId != null && count != null) {
                pos = parseXrefEntries(pdfText, pos, startId, count, offsets, seenIds)
            } else {
                parsing = false
            }
        }
        return pos
    }

    private fun parseXrefEntries(
        pdfText: String,
        startPos: Int,
        startId: Int,
        count: Int,
        offsets: MutableMap<Int, Int>,
        seenIds: MutableSet<Int>
    ): Int {
        var pos = startPos
        for (index in 0 until count) {
            if (pos >= pdfText.length) break
            val lineEnd = PdfTextScanner.findLineEnd(pdfText, pos)
            val tokens = pdfText.substring(pos, lineEnd).trim().split(WHITESPACE_SPLIT_REGEX)
            recordXrefEntry(tokens, startId + index, offsets, seenIds)
            pos = PdfTextScanner.skipWhitespace(pdfText, lineEnd)
        }
        return pos
    }

    private fun recordXrefEntry(
        tokens: List<String>,
        objId: Int,
        offsets: MutableMap<Int, Int>,
        seenIds: MutableSet<Int>
    ) {
        if (tokens.size < XREF_ENTRY_TOKENS) return
        if (seenIds.add(objId) && tokens[2] == IN_USE_FLAG) {
            tokens[0].toIntOrNull()?.let { offsets[objId] = it }
        }
    }

    private fun collectPageIdsRecursive(
        pdfText: String,
        objectOffsets: Map<Int, Int>,
        nodeObjId: Int,
        visited: MutableSet<Int>,
        result: MutableList<Int>
    ) {
        if (!visited.add(nodeObjId)) return
        val dict = readObjectDictionary(pdfText, objectOffsets, nodeObjId) ?: return
        val kidsMatch = KIDS_ARRAY_REGEX.find(dict)
        if (kidsMatch != null) {
            val kidIds = OBJ_ID_FROM_REF_REGEX.findAll(kidsMatch.groupValues[1])
                .mapNotNull { it.groupValues[1].toIntOrNull() }
                .toList()
            for (kidId in kidIds) {
                collectPageIdsRecursive(pdfText, objectOffsets, kidId, visited, result)
            }
        } else {
            result.add(nodeObjId)
        }
    }
}

private object PdfTextScanner {

    private const val DICT_OPEN = "<<"
    private const val DICT_CLOSE = ">>"
    private const val COMMENT_CHAR = '%'
    private const val ESCAPE_CHAR = '\\'
    private const val STRING_OPEN = '('
    private const val STRING_CLOSE = ')'
    private const val HEX_OPEN = '<'
    private const val HEX_CLOSE = '>'

    fun extractBalancedDictionary(text: String, startFrom: Int): String? {
        val dictStart = text.indexOf(DICT_OPEN, startFrom)
        if (dictStart < 0) return null

        var index = dictStart
        var depth = 0
        while (index < text.length) {
            val ch = text[index]
            when {
                ch == STRING_OPEN -> index = skipLiteralString(text, index)
                ch == COMMENT_CHAR -> index = findLineEnd(text, index)
                text.startsWith(DICT_OPEN, index) -> {
                    depth++
                    index += DICT_OPEN.length
                }
                text.startsWith(DICT_CLOSE, index) -> {
                    depth--
                    index += DICT_CLOSE.length
                    if (depth == 0) return text.substring(dictStart, index)
                }
                ch == HEX_OPEN -> index = skipHexString(text, index)
                else -> index++
            }
        }
        return null
    }

    fun skipWhitespace(text: String, startIndex: Int): Int {
        var index = startIndex
        while (index < text.length && text[index].isWhitespace()) {
            index++
        }
        return index
    }

    fun findLineEnd(text: String, startIndex: Int): Int {
        var index = startIndex
        while (index < text.length && text[index] != '\n' && text[index] != '\r') {
            index++
        }
        return index
    }

    private fun skipLiteralString(text: String, startIndex: Int): Int {
        var index = startIndex + 1
        var parenDepth = 1
        while (index < text.length && parenDepth > 0) {
            when (text[index]) {
                ESCAPE_CHAR -> index += 2
                STRING_OPEN -> {
                    parenDepth++
                    index++
                }
                STRING_CLOSE -> {
                    parenDepth--
                    index++
                }
                else -> index++
            }
        }
        return index
    }

    private fun skipHexString(text: String, startIndex: Int): Int {
        val end = text.indexOf(HEX_CLOSE, startIndex + 1)
        return if (end < 0) text.length else end + 1
    }
}
