package es.pedrazamiguez.splittrip.features.expense.presentation.preview

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.User
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.UsersGroup
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewComplete
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewThemeWrapper
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewThemes
import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.features.expense.presentation.component.form.payment.FundingSourceSection
import es.pedrazamiguez.splittrip.features.expense.presentation.component.step.expense.FundingSourceStep
import es.pedrazamiguez.splittrip.features.expense.presentation.model.FundingSourceUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.AddExpenseUiState
import kotlinx.collections.immutable.persistentListOf

private val PREVIEW_GROUP_POCKET_SOURCE = FundingSourceUiModel(
    id = PayerType.GROUP.name,
    displayText = "Group Pocket",
    description = "The group's shared pool of money collected from member contributions. " +
        "Use this when paying from money set aside by the group.",
    icon = TablerIcons.Outline.UsersGroup
)

private val PREVIEW_YOUR_MONEY_SOURCE = FundingSourceUiModel(
    id = PayerType.USER.name,
    displayText = "Your Money",
    description = "You pay with your own money. SplitTrip tracks everyone's share so you're reimbursed later.",
    icon = TablerIcons.Outline.User
)

private val PREVIEW_FUNDING_SOURCES = persistentListOf(
    PREVIEW_GROUP_POCKET_SOURCE,
    PREVIEW_YOUR_MONEY_SOURCE
)

@PreviewThemes
@Composable
private fun FundingSourceSectionGroupPocketSelectedPreview() {
    PreviewThemeWrapper {
        FundingSourceSection(
            fundingSources = PREVIEW_FUNDING_SOURCES,
            selectedFundingSource = PREVIEW_GROUP_POCKET_SOURCE,
            onFundingSourceSelected = {},
            modifier = Modifier.padding(MaterialTheme.spacing.Default)
        )
    }
}

@PreviewThemes
@Composable
private fun FundingSourceSectionYourMoneySelectedPreview() {
    PreviewThemeWrapper {
        FundingSourceSection(
            fundingSources = PREVIEW_FUNDING_SOURCES,
            selectedFundingSource = PREVIEW_YOUR_MONEY_SOURCE,
            onFundingSourceSelected = {},
            modifier = Modifier.padding(MaterialTheme.spacing.Default)
        )
    }
}

@PreviewComplete
@Composable
private fun FundingSourceStepGroupPocketSelectedPreview() {
    PreviewThemeWrapper {
        FundingSourceStep(
            uiState = AddExpenseUiState(
                fundingSources = PREVIEW_FUNDING_SOURCES,
                selectedFundingSource = PREVIEW_GROUP_POCKET_SOURCE
            ),
            onEvent = {}
        )
    }
}

@PreviewComplete
@Composable
private fun FundingSourceStepYourMoneySelectedPreview() {
    PreviewThemeWrapper {
        FundingSourceStep(
            uiState = AddExpenseUiState(
                fundingSources = PREVIEW_FUNDING_SOURCES,
                selectedFundingSource = PREVIEW_YOUR_MONEY_SOURCE
            ),
            onEvent = {}
        )
    }
}
