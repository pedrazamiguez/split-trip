package es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.event

import es.pedrazamiguez.splittrip.domain.model.User

sealed interface CreateEditGroupUiEvent {
    data class NameChanged(val name: String) : CreateEditGroupUiEvent
    data class DescriptionChanged(val description: String) : CreateEditGroupUiEvent
    data class CurrencySelected(val code: String) : CreateEditGroupUiEvent
    data class ExtraCurrencyToggled(val code: String) : CreateEditGroupUiEvent
    data class MemberSearchQueryChanged(val query: String) : CreateEditGroupUiEvent
    data class MemberSelected(val user: User) : CreateEditGroupUiEvent
    data class MemberRemoved(val user: User) : CreateEditGroupUiEvent
    data class MemberScanned(val userId: String, val email: String) : CreateEditGroupUiEvent
    data class UnregisteredMemberDisplayNameChanged(
        val userId: String,
        val displayName: String
    ) : CreateEditGroupUiEvent
    data class GroupImagePicked(val uri: String) : CreateEditGroupUiEvent
    data object GroupImageRemoved : CreateEditGroupUiEvent
    data class ShowImageSourceSheet(val show: Boolean) : CreateEditGroupUiEvent
    data object NextStep : CreateEditGroupUiEvent
    data object PreviousStep : CreateEditGroupUiEvent
    data class JumpToStep(val stepIndex: Int) : CreateEditGroupUiEvent
    data object Submit : CreateEditGroupUiEvent
}
