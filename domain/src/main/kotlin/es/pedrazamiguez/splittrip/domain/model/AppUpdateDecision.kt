package es.pedrazamiguez.splittrip.domain.model

sealed interface AppUpdateDecision {
    data object None : AppUpdateDecision

    data class Optional(
        val availableVersionCode: Long,
        val availableVersion: SemanticVersion
    ) : AppUpdateDecision

    data class Forced(
        val availableVersionCode: Long,
        val availableVersion: SemanticVersion,
        val reason: ForcedUpdateReason
    ) : AppUpdateDecision
}

enum class ForcedUpdateReason {
    BELOW_MIN_SUPPORTED_VERSION,
    MAJOR_VERSION_UPDATE,
    PATCH_HOTFIX_UPDATE
}
