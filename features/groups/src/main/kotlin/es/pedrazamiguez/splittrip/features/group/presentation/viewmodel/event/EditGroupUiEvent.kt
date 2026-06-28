package es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.event

sealed interface EditGroupUiEvent {
    data class NameChanged(val name: String) : EditGroupUiEvent
    data class DescriptionChanged(val description: String) : EditGroupUiEvent
    data class CurrencySelected(val code: String) : EditGroupUiEvent
    data class ExtraCurrencyToggled(val code: String) : EditGroupUiEvent
    data class GroupImagePicked(val uri: String) : EditGroupUiEvent
    object GroupImageRemoved : EditGroupUiEvent
    data class ShowImageSourceSheet(val show: Boolean) : EditGroupUiEvent
    object SaveClicked : EditGroupUiEvent
}
