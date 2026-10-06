package es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.event

import es.pedrazamiguez.splittrip.features.group.presentation.model.leave.LeaveWizardStep

sealed interface GroupsUiEvent {
    data object LoadGroups : GroupsUiEvent
    data class ScrollPositionChanged(val index: Int, val offset: Int) : GroupsUiEvent
    data class DeleteGroup(val groupId: String) : GroupsUiEvent
    data class ArchiveGroup(val groupId: String) : GroupsUiEvent
    data class LeaveGroup(val groupId: String) : GroupsUiEvent
    data class WizardNextClicked(val groupId: String) : GroupsUiEvent
    data object WizardBackClicked : GroupsUiEvent
    data object WizardCancelled : GroupsUiEvent
    data object NavigateToYourBalanceClicked : GroupsUiEvent
    data class LeaveConfirmed(val groupId: String) : GroupsUiEvent
    data class WizardJumpToStepClicked(val step: LeaveWizardStep) : GroupsUiEvent
    data class SelectGroup(val groupId: String) : GroupsUiEvent
}
