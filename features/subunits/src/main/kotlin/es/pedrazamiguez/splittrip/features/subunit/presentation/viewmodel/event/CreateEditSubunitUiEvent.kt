package es.pedrazamiguez.splittrip.features.subunit.presentation.viewmodel.event

sealed interface CreateEditSubunitUiEvent {
    data class UpdateName(val name: String) : CreateEditSubunitUiEvent
    data class ToggleMember(val userId: String) : CreateEditSubunitUiEvent
    data class UpdateMemberShare(val userId: String, val share: String) : CreateEditSubunitUiEvent
    data class ToggleShareLock(val userId: String) : CreateEditSubunitUiEvent
    data object Save : CreateEditSubunitUiEvent

    // ── Wizard Navigation ────────────────────────────────────────────────
    data object NextStep : CreateEditSubunitUiEvent
    data object PreviousStep : CreateEditSubunitUiEvent
    data object CloseWizard : CreateEditSubunitUiEvent

    /** Jumps directly to a previously completed step by its zero-based index. */
    data class JumpToStep(val stepIndex: Int) : CreateEditSubunitUiEvent
}
