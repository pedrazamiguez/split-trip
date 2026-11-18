package es.pedrazamiguez.splittrip.features.contribution.presentation.preview

import androidx.compose.runtime.Composable
import es.pedrazamiguez.splittrip.core.designsystem.presentation.model.MemberOptionUiModel
import es.pedrazamiguez.splittrip.core.designsystem.presentation.model.SubunitOptionUiModel
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewComplete
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewNarrowThemes
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewThemeWrapper
import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.features.contribution.presentation.screen.AddContributionScreen
import es.pedrazamiguez.splittrip.features.contribution.presentation.viewmodel.state.AddContributionStep
import es.pedrazamiguez.splittrip.features.contribution.presentation.viewmodel.state.AddContributionUiState
import kotlinx.collections.immutable.persistentListOf

private val previewMembers = persistentListOf(
    MemberOptionUiModel(userId = "user-1", displayName = "Andrés", isCurrentUser = true),
    MemberOptionUiModel(userId = "user-2", displayName = "Ana", isCurrentUser = false),
    MemberOptionUiModel(userId = "user-3", displayName = "Luis", isCurrentUser = false)
)

private val previewSubunits = persistentListOf(
    SubunitOptionUiModel("subunit-1", "Couple A"),
    SubunitOptionUiModel("subunit-2", "Couple B")
)

@PreviewComplete
@Composable
private fun AddContributionAmountStepEmptyPreview() {
    PreviewThemeWrapper {
        AddContributionScreen(
            uiState = AddContributionUiState(
                currentStep = AddContributionStep.AMOUNT,
                groupCurrencyCode = "EUR",
                groupCurrencySymbol = "€"
            )
        )
    }
}

@PreviewComplete
@Composable
private fun AddContributionAmountStepFilledPreview() {
    PreviewThemeWrapper {
        AddContributionScreen(
            uiState = AddContributionUiState(
                currentStep = AddContributionStep.AMOUNT,
                amountInput = "300",
                groupCurrencyCode = "EUR",
                groupCurrencySymbol = "€"
            )
        )
    }
}

@PreviewComplete
@Composable
private fun AddContributionAmountStepErrorPreview() {
    PreviewThemeWrapper {
        AddContributionScreen(
            uiState = AddContributionUiState(
                currentStep = AddContributionStep.AMOUNT,
                amountInput = "",
                amountError = true,
                groupCurrencyCode = "EUR",
                groupCurrencySymbol = "€"
            )
        )
    }
}

@PreviewComplete
@Composable
private fun AddContributionScopeStepGroupPreview() {
    PreviewThemeWrapper {
        AddContributionScreen(
            uiState = AddContributionUiState(
                currentStep = AddContributionStep.SCOPE,
                amountInput = "300",
                groupCurrencyCode = "EUR",
                groupCurrencySymbol = "€",
                contributionScope = PayerType.GROUP,
                groupMembers = previewMembers,
                selectedMemberId = "user-1",
                selectedMemberDisplayName = "Andrés"
            )
        )
    }
}

@PreviewComplete
@Composable
private fun AddContributionScopeStepSubunitPreview() {
    PreviewThemeWrapper {
        AddContributionScreen(
            uiState = AddContributionUiState(
                currentStep = AddContributionStep.SCOPE,
                amountInput = "150",
                groupCurrencyCode = "EUR",
                groupCurrencySymbol = "€",
                contributionScope = PayerType.SUBUNIT,
                selectedSubunitId = "subunit-1",
                subunitOptions = previewSubunits,
                groupMembers = previewMembers,
                selectedMemberId = "user-1",
                selectedMemberDisplayName = "Andrés"
            )
        )
    }
}

@PreviewComplete
@Composable
private fun AddContributionScopeStepImpersonatedPreview() {
    PreviewThemeWrapper {
        AddContributionScreen(
            uiState = AddContributionUiState(
                currentStep = AddContributionStep.SCOPE,
                amountInput = "300",
                groupCurrencyCode = "EUR",
                groupCurrencySymbol = "€",
                contributionScope = PayerType.USER,
                groupMembers = previewMembers,
                selectedMemberId = "user-2",
                selectedMemberDisplayName = "Ana"
            )
        )
    }
}

@PreviewComplete
@Composable
private fun AddContributionReviewStepGroupPreview() {
    PreviewThemeWrapper {
        AddContributionScreen(
            uiState = AddContributionUiState(
                currentStep = AddContributionStep.REVIEW,
                amountInput = "300",
                groupCurrencyCode = "EUR",
                formattedAmountWithCurrency = "300,00\u00A0€",
                contributionScope = PayerType.GROUP,
                groupMembers = previewMembers,
                selectedMemberId = "user-1",
                selectedMemberDisplayName = "Andrés"
            )
        )
    }
}

@PreviewComplete
@Composable
private fun AddContributionReviewStepSubunitPreview() {
    PreviewThemeWrapper {
        AddContributionScreen(
            uiState = AddContributionUiState(
                currentStep = AddContributionStep.REVIEW,
                amountInput = "150",
                groupCurrencyCode = "EUR",
                formattedAmountWithCurrency = "150,00\u00A0€",
                contributionScope = PayerType.SUBUNIT,
                selectedSubunitId = "subunit-1",
                subunitOptions = previewSubunits,
                groupMembers = previewMembers,
                selectedMemberId = "user-1",
                selectedMemberDisplayName = "Andrés"
            )
        )
    }
}

@PreviewComplete
@Composable
private fun AddContributionReviewStepImpersonatedPreview() {
    PreviewThemeWrapper {
        AddContributionScreen(
            uiState = AddContributionUiState(
                currentStep = AddContributionStep.REVIEW,
                amountInput = "300",
                groupCurrencyCode = "EUR",
                formattedAmountWithCurrency = "300,00\u00A0€",
                contributionScope = PayerType.USER,
                groupMembers = previewMembers,
                selectedMemberId = "user-2",
                selectedMemberDisplayName = "Ana"
            )
        )
    }
}

@PreviewNarrowThemes
@Composable
private fun AddContributionReviewStepNarrowPreview() {
    PreviewThemeWrapper {
        AddContributionScreen(
            uiState = AddContributionUiState(
                currentStep = AddContributionStep.REVIEW,
                amountInput = "300",
                groupCurrencyCode = "EUR",
                formattedAmountWithCurrency = "300,00\u00A0€",
                contributionScope = PayerType.GROUP,
                groupMembers = previewMembers,
                selectedMemberId = "user-1",
                selectedMemberDisplayName = "Andrés"
            )
        )
    }
}
