package es.pedrazamiguez.splittrip.domain.model

enum class ModelPackTier(val rank: Int) {
    COMPACT(rank = 1),
    BALANCED(rank = 2),
    HIGH_QUALITY(rank = 3);

    companion object {
        fun fromCode(code: String?): ModelPackTier = when (code?.trim()?.lowercase()) {
            "compact", "lite", "small" -> COMPACT
            "high_quality", "high", "large", "pro" -> HIGH_QUALITY
            else -> BALANCED
        }
    }
}
