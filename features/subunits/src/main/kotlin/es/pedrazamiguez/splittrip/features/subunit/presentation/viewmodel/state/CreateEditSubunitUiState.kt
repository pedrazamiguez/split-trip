package es.pedrazamiguez.splittrip.features.subunit.presentation.viewmodel.state

import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.features.subunit.presentation.model.MemberUiModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf

data class CreateEditSubunitUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isEditing: Boolean = false,
    val name: String = "",
    val selectedMemberIds: ImmutableList<String> = persistentListOf(),
    val memberShares: Map<String, String> = emptyMap(),
    val lockedMemberIds: ImmutableSet<String> = persistentSetOf(),
    val availableMembers: ImmutableList<MemberUiModel> = persistentListOf(),
    val nameError: UiText? = null,
    val membersError: UiText? = null,
    val sharesError: UiText? = null,

    // ── Wizard ──────────────────────────────────────────────────────────
    val currentStep: CreateEditSubunitStep = CreateEditSubunitStep.NAME,
    val initialFormSnapshot: CreateEditSubunitFormSnapshot? = null
) {
    val isDirty: Boolean
        get() = initialFormSnapshot != null && toFormSnapshot() != initialFormSnapshot

    fun toFormSnapshot() = CreateEditSubunitFormSnapshot(
        name = name,
        selectedMemberIds = selectedMemberIds,
        memberShares = memberShares,
        lockedMemberIds = lockedMemberIds
    )
    val steps: List<CreateEditSubunitStep>
        get() = CreateEditSubunitStep.entries

    val currentStepIndex: Int
        get() = steps.indexOf(currentStep).coerceAtLeast(0)

    val canGoNext: Boolean
        get() = currentStepIndex < steps.lastIndex

    val isOnReviewStep: Boolean
        get() = currentStep == CreateEditSubunitStep.REVIEW

    val isCurrentStepValid: Boolean
        get() = when (currentStep) {
            CreateEditSubunitStep.NAME -> name.isNotBlank()
            CreateEditSubunitStep.MEMBERS -> selectedMemberIds.isNotEmpty()
            CreateEditSubunitStep.SHARES -> true
            CreateEditSubunitStep.REVIEW ->
                name.isNotBlank() && selectedMemberIds.isNotEmpty()
        }
}

data class CreateEditSubunitFormSnapshot(
    val name: String,
    val selectedMemberIds: ImmutableList<String>,
    val memberShares: Map<String, String>,
    val lockedMemberIds: ImmutableSet<String>
)
