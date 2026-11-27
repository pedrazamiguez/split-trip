package es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.event

sealed interface LocalAiModelPackUiEvent {
    data class RequestDownload(val packId: String? = null) : LocalAiModelPackUiEvent
    data class CancelDownload(val packId: String? = null) : LocalAiModelPackUiEvent
    data class RetryDownload(val packId: String? = null) : LocalAiModelPackUiEvent
    data object RequestDelete : LocalAiModelPackUiEvent
    data object ConfirmDelete : LocalAiModelPackUiEvent
    data object DismissDeleteDialog : LocalAiModelPackUiEvent
    data class ToggleWifiOnly(val enabled: Boolean) : LocalAiModelPackUiEvent
    data object RequestUpgrade : LocalAiModelPackUiEvent
}
