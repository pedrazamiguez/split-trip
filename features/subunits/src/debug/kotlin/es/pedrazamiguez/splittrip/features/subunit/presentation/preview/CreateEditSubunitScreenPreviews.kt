package es.pedrazamiguez.splittrip.features.subunit.presentation.preview

import androidx.compose.runtime.Composable
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewComplete
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewThemeWrapper
import es.pedrazamiguez.splittrip.features.subunit.presentation.screen.CreateEditSubunitScreen
import es.pedrazamiguez.splittrip.features.subunit.presentation.viewmodel.state.CreateEditSubunitStep
import es.pedrazamiguez.splittrip.features.subunit.presentation.viewmodel.state.CreateEditSubunitUiState
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf

@PreviewComplete
@Composable
fun CreateEditSubunitScreenLoadingPreview() {
    PreviewThemeWrapper {
        CreateEditSubunitScreen(
            uiState = CreateEditSubunitUiState(
                isLoading = true
            )
        )
    }
}

@PreviewComplete
@Composable
fun CreateSubunitScreenNameStepPreview() {
    PreviewThemeWrapper {
        CreateEditSubunitScreen(
            uiState = CreateEditSubunitUiState(
                isLoading = false,
                currentStep = CreateEditSubunitStep.NAME,
                name = "Beach House",
                availableMembers = PREVIEW_GROUP_MEMBERS
            )
        )
    }
}

@PreviewComplete
@Composable
fun CreateSubunitScreenMembersStepPreview() {
    PreviewThemeWrapper {
        CreateEditSubunitScreen(
            uiState = CreateEditSubunitUiState(
                isLoading = false,
                currentStep = CreateEditSubunitStep.MEMBERS,
                name = "Beach House",
                selectedMemberIds = persistentListOf("user-1", "user-2"),
                availableMembers = PREVIEW_GROUP_MEMBERS
            )
        )
    }
}

@PreviewComplete
@Composable
fun CreateSubunitScreenSharesStepPreview() {
    PreviewThemeWrapper {
        CreateEditSubunitScreen(
            uiState = CreateEditSubunitUiState(
                isLoading = false,
                currentStep = CreateEditSubunitStep.SHARES,
                name = "Beach House",
                selectedMemberIds = persistentListOf("user-1", "user-2"),
                memberShares = PREVIEW_MEMBER_SHARES,
                lockedMemberIds = persistentSetOf("user-1"),
                availableMembers = PREVIEW_GROUP_MEMBERS
            )
        )
    }
}

@PreviewComplete
@Composable
fun CreateSubunitScreenReviewStepPreview() {
    PreviewThemeWrapper {
        CreateEditSubunitScreen(
            uiState = CreateEditSubunitUiState(
                isLoading = false,
                currentStep = CreateEditSubunitStep.REVIEW,
                name = "Beach House",
                selectedMemberIds = persistentListOf("user-1", "user-2"),
                memberShares = PREVIEW_MEMBER_SHARES,
                availableMembers = PREVIEW_GROUP_MEMBERS
            )
        )
    }
}

@PreviewComplete
@Composable
fun EditSubunitScreenPreview() {
    PreviewThemeWrapper {
        CreateEditSubunitScreen(
            uiState = CreateEditSubunitUiState(
                isLoading = false,
                isEditing = true,
                currentStep = CreateEditSubunitStep.NAME,
                name = "Cantalobos",
                selectedMemberIds = persistentListOf("user-1", "user-2"),
                memberShares = PREVIEW_MEMBER_SHARES,
                availableMembers = PREVIEW_GROUP_MEMBERS
            )
        )
    }
}
