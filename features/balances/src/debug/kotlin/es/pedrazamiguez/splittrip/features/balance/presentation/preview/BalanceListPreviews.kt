package es.pedrazamiguez.splittrip.features.balance.presentation.preview

import androidx.compose.runtime.Composable
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewComplete
import es.pedrazamiguez.splittrip.features.balance.presentation.component.ContributionHistoryItem
import es.pedrazamiguez.splittrip.features.balance.presentation.component.GroupPocketBalanceCard

@PreviewComplete
@Composable
private fun GroupPocketBalanceCardPreview() {
    BalanceCardPreviewHelper {
        GroupPocketBalanceCard(balance = it)
    }
}

@PreviewComplete
@Composable
private fun GroupPocketBalanceCardNarrowPreview() {
    BalanceCardPreviewHelper {
        GroupPocketBalanceCard(balance = it)
    }
}

@PreviewComplete
@Composable
private fun GroupPocketBalanceCardEmptyPreview() {
    BalanceCardPreviewHelper(domainBalance = PREVIEW_POCKET_BALANCE_EMPTY) {
        GroupPocketBalanceCard(balance = it)
    }
}

@PreviewComplete
@Composable
private fun ContributionHistoryItemPreview() {
    ContributionItemPreviewHelper {
        ContributionHistoryItem(contribution = it)
    }
}

@PreviewComplete
@Composable
private fun LinkedContributionHistoryItemPreview() {
    ContributionItemPreviewHelper(domainContribution = PREVIEW_CONTRIBUTION_LINKED) {
        ContributionHistoryItem(contribution = it)
    }
}
