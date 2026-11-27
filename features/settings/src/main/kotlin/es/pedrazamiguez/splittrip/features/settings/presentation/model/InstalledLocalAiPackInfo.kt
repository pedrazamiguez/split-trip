package es.pedrazamiguez.splittrip.features.settings.presentation.model

import es.pedrazamiguez.splittrip.domain.model.LocalAiModelPackState
import es.pedrazamiguez.splittrip.domain.model.ModelPackStatus
import es.pedrazamiguez.splittrip.domain.model.ModelPackTier

data class InstalledLocalAiPackInfo(
    val packId: String,
    val displayName: String,
    val tier: ModelPackTier?
) {
    companion object {
        fun fromState(state: LocalAiModelPackState): InstalledLocalAiPackInfo? {
            val installedEntry = state.catalogEntries.firstOrNull { it.isInstalled }
                ?: state.installedPackId?.takeIf { it.isNotBlank() }?.let { installedId ->
                    state.catalogEntries.firstOrNull { it.config.packId == installedId }
                }
            if (installedEntry != null) {
                val config = installedEntry.config
                return InstalledLocalAiPackInfo(
                    packId = config.packId,
                    displayName = config.displayName.ifBlank { config.packId },
                    tier = config.tier
                )
            }
            val installedStatus = state.status as? ModelPackStatus.Installed ?: return null
            val fallbackPackId = installedStatus.packId.takeIf { it.isNotBlank() } ?: return null
            return InstalledLocalAiPackInfo(
                packId = fallbackPackId,
                displayName = fallbackPackId,
                tier = null
            )
        }
    }
}
