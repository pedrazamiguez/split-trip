package es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.state

import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip.PassportTagStyle

data class LocalAiModelPackUiState(
    val isLoading: Boolean = true,
    val statusBadge: UiText = UiText.DynamicString(""),
    val statusTagStyle: PassportTagStyle = PassportTagStyle.NEUTRAL,
    val statusTitle: UiText = UiText.DynamicString(""),
    val statusDescription: UiText = UiText.DynamicString(""),
    val packVersionLabel: UiText? = null,
    val packSizeLabel: String? = null,
    val isDownloading: Boolean = false,
    val isVerifying: Boolean = false,
    val progressFraction: Float = 0f,
    val progressLabel: UiText? = null,
    val isInstalled: Boolean = false,
    val isUpdateAvailable: Boolean = false,
    val isCompatible: Boolean = true,
    val compatibilityLabel: UiText = UiText.DynamicString(""),
    val compatibilityTagStyle: PassportTagStyle = PassportTagStyle.NEUTRAL,
    val activeEngineLabel: UiText = UiText.DynamicString(""),
    val isEntitled: Boolean = false,
    val isWifiOnly: Boolean = true,
    val showDownloadButton: Boolean = false,
    val showRetryButton: Boolean = false,
    val showCancelButton: Boolean = false,
    val showDeleteButton: Boolean = false,
    val showUpgradeButton: Boolean = false,
    val errorMessage: UiText? = null,
    val showDeleteConfirmation: Boolean = false
)
