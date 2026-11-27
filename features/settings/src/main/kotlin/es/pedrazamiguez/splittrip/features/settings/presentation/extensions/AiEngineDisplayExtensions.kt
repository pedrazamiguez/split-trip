package es.pedrazamiguez.splittrip.features.settings.presentation.extensions

import androidx.annotation.StringRes
import es.pedrazamiguez.splittrip.domain.model.ModelPackTier
import es.pedrazamiguez.splittrip.features.settings.R
import es.pedrazamiguez.splittrip.features.settings.presentation.model.InstalledLocalAiPackInfo

@StringRes
fun ModelPackTier.toLabelRes(): Int = when (this) {
    ModelPackTier.COMPACT -> R.string.local_ai_pack_tier_compact
    ModelPackTier.BALANCED -> R.string.local_ai_pack_tier_balanced
    ModelPackTier.HIGH_QUALITY -> R.string.local_ai_pack_tier_high_quality
}

fun formatLiteRtDisplayLabel(
    baseLabel: String,
    installedPack: InstalledLocalAiPackInfo?,
    localizedTierLabel: String?
): String {
    if (installedPack == null) return baseLabel
    val detail = formatInstalledPackDetail(installedPack, localizedTierLabel)
    return "$baseLabel · $detail"
}

private fun formatInstalledPackDetail(
    installedPack: InstalledLocalAiPackInfo,
    localizedTierLabel: String?
): String {
    val trimmedName = installedPack.displayName.trim()
    val trimmedTier = localizedTierLabel?.trim()?.takeIf { it.isNotEmpty() }
    if (trimmedName.isEmpty()) {
        return trimmedTier ?: installedPack.packId
    }
    if (trimmedTier == null) {
        return trimmedName
    }
    val hasLocalizedTier = trimmedName.contains(trimmedTier, ignoreCase = true)
    val hasEnglishTier = installedPack.tier
        ?.englishKeyword()
        ?.let { keyword -> trimmedName.contains(keyword, ignoreCase = true) }
        ?: false
    return if (hasLocalizedTier || hasEnglishTier) {
        trimmedName
    } else {
        "$trimmedTier ($trimmedName)"
    }
}

private fun ModelPackTier.englishKeyword(): String = when (this) {
    ModelPackTier.COMPACT -> "compact"
    ModelPackTier.BALANCED -> "balanced"
    ModelPackTier.HIGH_QUALITY -> "high quality"
}
