package es.pedrazamiguez.splittrip.domain.model

import es.pedrazamiguez.splittrip.domain.enums.AiEngineType

data class LocalAiModelPackState(
    val status: ModelPackStatus,
    val compatibility: DeviceCompatibilityResult,
    val isEntitled: Boolean,
    val isWifiOnly: Boolean,
    val isAiCoreSupported: Boolean,
    val activeEngine: AiEngineType,
    val catalogEntries: List<ModelPackCatalogEntry> = emptyList(),
    val recommendedPackId: String? = null,
    val installedPackId: String? = null
) {
    /**
     * True when at least one enabled catalog pack is compatible with this device.
     * Falls back to [compatibility].isCompatible when [catalogEntries] is empty.
     */
    val hasCompatiblePack: Boolean
        get() = if (catalogEntries.isNotEmpty()) {
            catalogEntries.any { it.compatibility.isCompatible }
        } else {
            compatibility.isCompatible
        }

    /**
     * True when the non-intrusive opt-in banner in the expense flow may be shown:
     * AICore is unsupported on the device, user has Pro entitlement, at least one enabled pack
     * is compatible, and no pack is yet installed.
     */
    val isOptInBannerEligible: Boolean
        get() = !isAiCoreSupported &&
            isEntitled &&
            hasCompatiblePack &&
            status is ModelPackStatus.NotInstalled
}
