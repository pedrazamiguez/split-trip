package es.pedrazamiguez.splittrip.features.balance.presentation.preview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewComplete
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewNarrowThemes
import es.pedrazamiguez.splittrip.features.balance.presentation.component.ContributionHistoryItem
import es.pedrazamiguez.splittrip.features.balance.presentation.component.GroupPocketBalanceCard

private const val NARROW_LARGE_FONT_SCALE = 1.3f

@PreviewComplete
@Composable
private fun GroupPocketBalanceCardPreview() {
    BalanceCardPreviewHelper {
        GroupPocketBalanceCard(balance = it)
    }
}

@PreviewNarrowThemes
@Composable
private fun GroupPocketBalanceCardNarrowPreview() {
    BalanceCardPreviewHelper {
        GroupPocketBalanceCard(balance = it)
    }
}

@PreviewNarrowThemes
@Composable
private fun GroupPocketBalanceCardNarrowLargeFontPreview() {
    val currentDensity = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(
            density = currentDensity.density,
            fontScale = NARROW_LARGE_FONT_SCALE
        )
    ) {
        BalanceCardPreviewHelper {
            GroupPocketBalanceCard(balance = it)
        }
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
