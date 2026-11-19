package es.pedrazamiguez.splittrip.domain.model

data class AppUpdatePolicy(
    val inAppUpdatesEnabled: Boolean,
    val minSupportedVersionCode: Long,
    val autoForcePatchUpdates: Boolean,
    val optionalUpdateVersionCodes: Set<Long>,
    val optionalUpdateCooldownHours: Long
)
