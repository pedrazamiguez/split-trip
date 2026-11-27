package es.pedrazamiguez.splittrip.features.settings.presentation.model

import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip.PassportTagStyle

enum class LocalAiModelPackTierAction {
    DOWNLOAD,
    SWITCH,
    UPDATE,
    CANCEL,
    RETRY,
    NONE
}

data class LocalAiModelPackTierUiModel(
    val packId: String,
    val displayName: UiText,
    val tierLabel: UiText,
    val versionLabel: UiText,
    val sizeLabel: String,
    val requirementsLabel: UiText,
    val statusBadge: UiText,
    val statusTagStyle: PassportTagStyle,
    val isRecommended: Boolean,
    val isInstalled: Boolean,
    val isCompatible: Boolean,
    val isDownloading: Boolean,
    val isVerifying: Boolean,
    val progressFraction: Float,
    val progressLabel: UiText?,
    val incompatibilityReason: UiText?,
    val action: LocalAiModelPackTierAction,
    val actionLabel: UiText?,
    val isActionEnabled: Boolean
)
