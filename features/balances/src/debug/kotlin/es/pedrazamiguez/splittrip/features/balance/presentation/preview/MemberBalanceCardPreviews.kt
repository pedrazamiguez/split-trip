package es.pedrazamiguez.splittrip.features.balance.presentation.preview

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewComplete
import es.pedrazamiguez.splittrip.features.balance.presentation.component.MemberBalanceCard
import es.pedrazamiguez.splittrip.features.balance.presentation.component.MemberBalanceSummaryRow

@PreviewComplete
@Composable
private fun MemberBalanceCardPositivePreview() {
    MemberBalanceItemPreviewHelper(domainBalance = PREVIEW_MEMBER_BALANCE_POSITIVE) { memberBalance ->
        MemberBalanceCard(
            expandedStateDesc = "Collapsed",
            toggleContentDesc = "Expand",
            onToggle = {}
        ) {
            MemberBalanceSummaryRow(
                memberBalance = memberBalance,
                displayName = memberBalance.displayName,
                balanceColor = MaterialTheme.colorScheme.primary,
                isExpanded = false,
                toggleContentDesc = "Expand"
            )
        }
    }
}

@PreviewComplete
@Composable
private fun MemberBalanceCardNegativePreview() {
    MemberBalanceItemPreviewHelper(domainBalance = PREVIEW_MEMBER_BALANCE_NEGATIVE) { memberBalance ->
        MemberBalanceCard(
            expandedStateDesc = "Collapsed",
            toggleContentDesc = "Expand",
            onToggle = {}
        ) {
            MemberBalanceSummaryRow(
                memberBalance = memberBalance,
                displayName = memberBalance.displayName,
                balanceColor = MaterialTheme.colorScheme.error,
                isExpanded = false,
                toggleContentDesc = "Expand"
            )
        }
    }
}

@PreviewComplete
@Composable
private fun MemberBalanceCardWithCashPreview() {
    MemberBalanceItemPreviewHelper(domainBalance = PREVIEW_MEMBER_BALANCE_NEGATIVE_CASH) { memberBalance ->
        MemberBalanceCard(
            expandedStateDesc = "Collapsed",
            toggleContentDesc = "Expand",
            onToggle = {}
        ) {
            MemberBalanceSummaryRow(
                memberBalance = memberBalance,
                displayName = memberBalance.displayName,
                balanceColor = MaterialTheme.colorScheme.error,
                isExpanded = false,
                toggleContentDesc = "Expand"
            )
        }
    }
}
