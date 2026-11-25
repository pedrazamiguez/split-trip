package es.pedrazamiguez.splittrip.domain.service.category

import java.text.Normalizer
import java.util.Locale

object CategoryTextNormalizer {
    fun normalize(input: String): String? {
        if (input.isBlank()) return null
        val decomposed = Normalizer.normalize(input, Normalizer.Form.NFD)
        val builder = StringBuilder(decomposed.length)
        var pendingSpace = false

        for (ch in decomposed) {
            val charType = Character.getType(ch)
            val isCombiningMark = charType == Character.NON_SPACING_MARK.toInt() ||
                charType == Character.COMBINING_SPACING_MARK.toInt() ||
                charType == Character.ENCLOSING_MARK.toInt()
            if (isCombiningMark) continue

            if (ch.isLetterOrDigit()) {
                if (pendingSpace && builder.isNotEmpty()) {
                    builder.append(' ')
                }
                pendingSpace = false
                builder.append(ch)
            } else if (builder.isNotEmpty()) {
                pendingSpace = true
            }
        }

        return builder.toString().lowercase(Locale.ROOT).ifBlank { null }
    }
}
