package es.pedrazamiguez.splittrip.domain.model

data class ModelPackCatalogEntry(
    val config: ModelPackConfig,
    val compatibility: DeviceCompatibilityResult,
    val status: ModelPackStatus,
    val isRecommended: Boolean,
    val isInstalled: Boolean,
    val isActiveDownload: Boolean
)
