package es.pedrazamiguez.splittrip.features.contribution.presentation.preview

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.model.MemberDisplay
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewComplete
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewThemeWrapper
import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.domain.enums.SyncStatus
import es.pedrazamiguez.splittrip.features.contribution.presentation.component.detail.ContributionHeroSection
import es.pedrazamiguez.splittrip.features.contribution.presentation.component.detail.ContributionProvenanceSection
import es.pedrazamiguez.splittrip.features.contribution.presentation.component.detail.ContributionScopeSection
import es.pedrazamiguez.splittrip.features.contribution.presentation.model.ContributionDetailUiModel
import es.pedrazamiguez.splittrip.features.contribution.presentation.screen.ContributionDetailScreen
import es.pedrazamiguez.splittrip.features.contribution.presentation.viewmodel.state.ContributionDetailUiState

private val PREVIEW_CONTRIBUTION_GROUP = ContributionDetailUiModel(
    id = "contrib-1",
    groupId = "group-1",
    formattedAmount = "150,00 €",
    dateText = "12 Oct 2024",
    createdAtText = "Created 12 Oct 2024, 14:30",
    memberDisplay = MemberDisplay.Active("Andrés", ""),
    isCurrentUser = true,
    contributorName = "Andrés",
    contributedByText = "Contributed by Andrés",
    createdByText = "Recorded by Andrés",
    scopeLabel = "Group",
    scopeDescription = "Shared across all group members",
    scopeType = PayerType.GROUP,
    syncStatus = SyncStatus.SYNCED
)

private val PREVIEW_CONTRIBUTION_SUBUNIT = ContributionDetailUiModel(
    id = "contrib-2",
    groupId = "group-1",
    formattedAmount = "75,00 €",
    dateText = "14 Oct 2024",
    createdAtText = "Created 14 Oct 2024, 10:15",
    memberDisplay = MemberDisplay.Active("María", ""),
    contributorName = "María",
    contributedByText = "Contributed by María",
    createdByText = "Recorded by Andrés",
    scopeLabel = "Cantalobos",
    scopeDescription = "For subunit Cantalobos",
    scopeType = PayerType.SUBUNIT,
    subunitName = "Cantalobos",
    syncStatus = SyncStatus.SYNCED
)

private val PREVIEW_CONTRIBUTION_PERSONAL = ContributionDetailUiModel(
    id = "contrib-3",
    groupId = "group-1",
    formattedAmount = "$50.00",
    formattedEquivalentAmount = "45,00 €",
    isForeignCurrency = true,
    sourceCurrency = "USD",
    formattedExchangeRate = "1 EUR = 1.11 USD",
    dateText = "15 Oct 2024",
    createdAtText = "Created 15 Oct 2024, 18:00",
    memberDisplay = MemberDisplay.Active("Antonio", ""),
    contributorName = "Antonio",
    contributedByText = "Contributed by Antonio",
    createdByText = "Recorded by Antonio",
    scopeLabel = "Personal",
    scopeDescription = "Personal contribution",
    scopeType = PayerType.USER,
    syncStatus = SyncStatus.SYNCED
)

@PreviewComplete
@Composable
fun ContributionDetailScreenLoadingPreview() {
    PreviewThemeWrapper {
        ContributionDetailScreen(
            uiState = ContributionDetailUiState(isLoading = true)
        )
    }
}

@PreviewComplete
@Composable
fun ContributionDetailScreenErrorPreview() {
    PreviewThemeWrapper {
        ContributionDetailScreen(
            uiState = ContributionDetailUiState(
                isLoading = false,
                hasError = true
            )
        )
    }
}

@PreviewComplete
@Composable
fun ContributionDetailScreenGroupScopePreview() {
    PreviewThemeWrapper {
        ContributionDetailScreen(
            uiState = ContributionDetailUiState(
                isLoading = false,
                contribution = PREVIEW_CONTRIBUTION_GROUP
            )
        )
    }
}

@PreviewComplete
@Composable
fun ContributionDetailScreenSubunitScopePreview() {
    PreviewThemeWrapper {
        ContributionDetailScreen(
            uiState = ContributionDetailUiState(
                isLoading = false,
                contribution = PREVIEW_CONTRIBUTION_SUBUNIT
            )
        )
    }
}

@PreviewComplete
@Composable
fun ContributionDetailScreenPersonalScopePreview() {
    PreviewThemeWrapper {
        ContributionDetailScreen(
            uiState = ContributionDetailUiState(
                isLoading = false,
                contribution = PREVIEW_CONTRIBUTION_PERSONAL
            )
        )
    }
}

@PreviewComplete
@Composable
fun ContributionHeroSectionPreview() {
    PreviewThemeWrapper {
        ContributionHeroSection(
            contribution = PREVIEW_CONTRIBUTION_GROUP,
            modifier = Modifier.padding(MaterialTheme.spacing.Default)
        )
    }
}

@PreviewComplete
@Composable
fun ContributionProvenanceSectionPreview() {
    PreviewThemeWrapper {
        ContributionProvenanceSection(
            contribution = PREVIEW_CONTRIBUTION_GROUP,
            modifier = Modifier.padding(MaterialTheme.spacing.Default)
        )
    }
}

@PreviewComplete
@Composable
fun ContributionScopeSectionPreview() {
    PreviewThemeWrapper {
        ContributionScopeSection(
            contribution = PREVIEW_CONTRIBUTION_GROUP,
            modifier = Modifier.padding(MaterialTheme.spacing.Default)
        )
    }
}
