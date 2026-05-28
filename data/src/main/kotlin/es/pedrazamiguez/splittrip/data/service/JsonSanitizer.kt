package es.pedrazamiguez.splittrip.data.service

object JsonSanitizer {
    /**
     * Sanitizes raw model outputs by stripping markdown code fences and conversational filler,
     * extracting the raw JSON block between the first '{' and last '}' characters.
     */
    fun sanitize(rawInput: String): String {
        val clean = stripCodeFences(rawInput.trim())

        val firstBrace = clean.indexOf('{')
        val lastBrace = clean.lastIndexOf('}')
        if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
            return clean.substring(firstBrace, lastBrace + 1).trim()
        }

        return clean.trim()
    }

    private fun stripCodeFences(input: String): String {
        if (!input.contains("```")) return input

        val jsonStartIndex = input.indexOf("```json")
        if (jsonStartIndex != -1) {
            val contentStart = jsonStartIndex + 7
            val blockEnd = input.indexOf("```", contentStart)
            return if (blockEnd != -1) {
                input.substring(contentStart, blockEnd).trim()
            } else {
                input.substring(contentStart).trim()
            }
        }

        val blockStart = input.indexOf("```")
        if (blockStart != -1) {
            val contentStart = blockStart + 3
            val blockEnd = input.indexOf("```", contentStart)
            return if (blockEnd != -1) {
                input.substring(contentStart, blockEnd).trim()
            } else {
                input.substring(contentStart).trim()
            }
        }

        return input
    }
}
