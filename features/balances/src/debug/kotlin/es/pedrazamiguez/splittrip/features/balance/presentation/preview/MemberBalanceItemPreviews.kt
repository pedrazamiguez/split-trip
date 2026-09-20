package es.pedrazamiguez.splittrip.features.balance.presentation.preview

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewComplete
import es.pedrazamiguez.splittrip.features.balance.presentation.component.MemberBalanceItem

@PreviewComplete
@Composable
private fun MemberBalanceItemPositivePreview() {
    MemberBalanceItemPreviewHelper(domainBalance = PREVIEW_MEMBER_BALANCE_POSITIVE) {
        MemberBalanceItem(
            memberBalance = it,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@PreviewComplete
@Composable
private fun MemberBalanceItemNegativePreview() {
    MemberBalanceItemPreviewHelper(domainBalance = PREVIEW_MEMBER_BALANCE_NEGATIVE) {
        MemberBalanceItem(
            memberBalance = it,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@PreviewComplete
@Composable
private fun MemberBalanceItemNegativeCashPreview() {
    MemberBalanceItemPreviewHelper(domainBalance = PREVIEW_MEMBER_BALANCE_NEGATIVE_CASH) {
        MemberBalanceItem(
            memberBalance = it,
            modifier = Modifier.padding(16.dp)
        )
    }
}
