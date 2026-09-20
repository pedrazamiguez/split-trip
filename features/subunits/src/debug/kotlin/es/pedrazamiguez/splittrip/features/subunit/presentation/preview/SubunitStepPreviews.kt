package es.pedrazamiguez.splittrip.features.subunit.presentation.preview

import androidx.compose.runtime.Composable
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewComplete
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewThemeWrapper
import es.pedrazamiguez.splittrip.features.subunit.presentation.component.step.subunit.SubunitMembersStep
import es.pedrazamiguez.splittrip.features.subunit.presentation.component.step.subunit.SubunitNameStep
import es.pedrazamiguez.splittrip.features.subunit.presentation.component.step.subunit.SubunitReviewStep
import es.pedrazamiguez.splittrip.features.subunit.presentation.component.step.subunit.SubunitSharesStep
import es.pedrazamiguez.splittrip.features.subunit.presentation.viewmodel.state.CreateEditSubunitStep
import es.pedrazamiguez.splittrip.features.subunit.presentation.viewmodel.state.CreateEditSubunitUiState
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf

@PreviewComplete
@Composable
fun SubunitNameStepPreview() {
    PreviewThemeWrapper {
        SubunitNameStep(
            uiState = CreateEditSubunitUiState(
                currentStep = CreateEditSubunitStep.NAME,
                name = "Beach House"
            ),
            onEvent = {}
        )
    }
}

@PreviewComplete
@Composable
fun SubunitMembersStepPreview() {
    PreviewThemeWrapper {
        SubunitMembersStep(
            uiState = CreateEditSubunitUiState(
                currentStep = CreateEditSubunitStep.MEMBERS,
                name = "Beach House",
                selectedMemberIds = persistentListOf("user-1", "user-2"),
                availableMembers = PREVIEW_GROUP_MEMBERS
            ),
            onEvent = {}
        )
    }
}

@PreviewComplete
@Composable
fun SubunitSharesStepPreview() {
    PreviewThemeWrapper {
        SubunitSharesStep(
            uiState = CreateEditSubunitUiState(
                currentStep = CreateEditSubunitStep.SHARES,
                name = "Beach House",
                selectedMemberIds = persistentListOf("user-1", "user-2"),
                memberShares = PREVIEW_MEMBER_SHARES,
                lockedMemberIds = persistentSetOf("user-1"),
                availableMembers = PREVIEW_GROUP_MEMBERS
            ),
            onEvent = {}
        )
    }
}

@PreviewComplete
@Composable
fun SubunitReviewStepPreview() {
    PreviewThemeWrapper {
        SubunitReviewStep(
            uiState = CreateEditSubunitUiState(
                currentStep = CreateEditSubunitStep.REVIEW,
                name = "Beach House",
                selectedMemberIds = persistentListOf("user-1", "user-2"),
                memberShares = PREVIEW_MEMBER_SHARES,
                availableMembers = PREVIEW_GROUP_MEMBERS
            )
        )
    }
}
