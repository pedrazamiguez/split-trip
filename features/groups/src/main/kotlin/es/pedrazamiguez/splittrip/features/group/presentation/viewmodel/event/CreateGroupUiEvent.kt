package es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.event

import es.pedrazamiguez.splittrip.domain.model.User

sealed interface CreateGroupUiEvent {
    data class NameChanged(val name: String) : CreateGroupUiEvent
    data class CurrencySelected(val code: String) : CreateGroupUiEvent
    data class ExtraCurrencyToggled(val code: String) : CreateGroupUiEvent
    data class DescriptionChanged(val description: String) : CreateGroupUiEvent
    data class MemberSearchQueryChanged(val query: String) : CreateGroupUiEvent
    data class MemberSelected(val user: User) : CreateGroupUiEvent
    data class MemberRemoved(val user: User) : CreateGroupUiEvent
    data class MemberScanned(val userId: String, val email: String) : CreateGroupUiEvent
    data object SubmitCreateGroup : CreateGroupUiEvent

    // ── Wizard Navigation ────────────────────────────────────────────────
    data object NextStep : CreateGroupUiEvent
    data object PreviousStep : CreateGroupUiEvent

    /** Jumps directly to a previously completed step by its zero-based index. */
    data class JumpToStep(val stepIndex: Int) : CreateGroupUiEvent

    // ── Image Selection ──────────────────────────────────────────────────
    data class GroupImagePicked(val uri: String) : CreateGroupUiEvent
    data object GroupImageRemoved : CreateGroupUiEvent
    data class ShowImageSourceSheet(val show: Boolean) : CreateGroupUiEvent
}
