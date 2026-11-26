package es.pedrazamiguez.splittrip.domain.model

import es.pedrazamiguez.splittrip.domain.enums.AiEngineType

data class LocalAiModelPackState(
    val status: ModelPackStatus,
    val compatibility: DeviceCompatibilityResult,
    val isEntitled: Boolean,
    val isWifiOnly: Boolean,
    val isAiCoreSupported: Boolean,
    val activeEngine: AiEngineType
) {
    /**
     * True when the non-intrusive opt-in banner in the expense flow may be shown:
     * user has Pro entitlement, device is compatible, and the pack is not yet installed.
     */
    val isOptInBannerEligible: Boolean
        get() = isEntitled &&
            compatibility.isCompatible &&
            status is ModelPackStatus.NotInstalled
}
