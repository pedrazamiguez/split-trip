package es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.event

sealed interface LocalAiModelPackUiEvent {
    data object RequestDownload : LocalAiModelPackUiEvent
    data object CancelDownload : LocalAiModelPackUiEvent
    data object RetryDownload : LocalAiModelPackUiEvent
    data object RequestDelete : LocalAiModelPackUiEvent
    data object ConfirmDelete : LocalAiModelPackUiEvent
    data object DismissDeleteDialog : LocalAiModelPackUiEvent
    data class ToggleWifiOnly(val enabled: Boolean) : LocalAiModelPackUiEvent
    data object RequestUpgrade : LocalAiModelPackUiEvent
}
