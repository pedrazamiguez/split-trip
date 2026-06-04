package es.pedrazamiguez.splittrip.features.balance.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Sitemap
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.User
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.UsersGroup
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.CaptionText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.SecondaryBodyText
import es.pedrazamiguez.splittrip.features.balance.R
import es.pedrazamiguez.splittrip.features.balance.presentation.model.CashWithdrawalUiModel

@Suppress("CognitiveComplexMethod")
@Composable
internal fun WithdrawalDetailColumn(
    withdrawal: CashWithdrawalUiModel,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = if (withdrawal.isCurrentUser) {
                stringResource(R.string.balances_cash_withdrawal_by_you)
            } else {
                stringResource(R.string.balances_cash_withdrawal_by, withdrawal.displayName)
            },
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )

        if (!withdrawal.title.isNullOrBlank()) {
            SecondaryBodyText(text = withdrawal.title)
        }

        if (withdrawal.createdByDisplayName != null) {
            SecondaryBodyText(
                text = stringResource(R.string.balances_logged_by, withdrawal.createdByDisplayName),
                maxLines = Int.MAX_VALUE
            )
        }

        if (withdrawal.scopeLabel != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.ExtraSmall)
            ) {
                Icon(
                    imageVector = when {
                        withdrawal.isSubunitWithdrawal -> TablerIcons.Outline.Sitemap
                        withdrawal.isGroupWithdrawal -> TablerIcons.Outline.UsersGroup
                        else -> TablerIcons.Outline.User
                    },
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.tertiary
                )
                CaptionText(
                    text = withdrawal.scopeLabel,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }

        if (withdrawal.dateText.isNotBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            SecondaryBodyText(text = withdrawal.dateText, maxLines = Int.MAX_VALUE)
        }
    }
}
