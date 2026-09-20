package es.pedrazamiguez.splittrip.features.balance.presentation.preview

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewComplete
import es.pedrazamiguez.splittrip.features.balance.presentation.component.CashWithdrawalHistoryItem

@PreviewComplete
@Composable
private fun CashWithdrawalGroupPreview() {
    CashWithdrawalItemPreviewHelper(domainWithdrawal = PREVIEW_CASH_WITHDRAWAL_GROUP) {
        CashWithdrawalHistoryItem(
            withdrawal = it,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@PreviewComplete
@Composable
private fun CashWithdrawalSubunitPreview() {
    CashWithdrawalItemPreviewHelper(domainWithdrawal = PREVIEW_CASH_WITHDRAWAL_SUBUNIT) {
        CashWithdrawalHistoryItem(
            withdrawal = it,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@PreviewComplete
@Composable
private fun CashWithdrawalPersonalPreview() {
    CashWithdrawalItemPreviewHelper(domainWithdrawal = PREVIEW_CASH_WITHDRAWAL_PERSONAL) {
        CashWithdrawalHistoryItem(
            withdrawal = it,
            modifier = Modifier.padding(16.dp)
        )
    }
}
