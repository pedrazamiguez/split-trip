package es.pedrazamiguez.splittrip.features.balance.presentation.component

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import es.pedrazamiguez.splittrip.core.designsystem.constant.UiConstants
import es.pedrazamiguez.splittrip.core.designsystem.extension.debouncedClickableNoRipple
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.AnimatedAmount
import es.pedrazamiguez.splittrip.features.balance.R
import es.pedrazamiguez.splittrip.features.balance.presentation.model.BalanceMetricType
import es.pedrazamiguez.splittrip.features.balance.presentation.model.GroupPocketBalanceUiModel

@Composable
internal fun PocketBalanceMainSection(
    balance: GroupPocketBalanceUiModel,
    shouldAnimateBalance: Boolean,
    previousBalance: String,
    balanceRollingUp: Boolean,
    onBalanceAnimationComplete: () -> Unit,
    modifier: Modifier = Modifier,
    onShowMetricInfo: (BalanceMetricType) -> Unit = {}
) {
    BoxWithConstraints(modifier = modifier) {
        val isNarrowScreen = maxWidth < UiConstants.NARROW_SCREEN_BREAKPOINT
        val amountStyle = if (isNarrowScreen) {
            MaterialTheme.typography.headlineMedium
        } else {
            MaterialTheme.typography.displaySmall
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (balance.groupName.isNotBlank()) {
                Text(
                    text = balance.groupName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.Medium))
            }
            Column(
                modifier = Modifier.debouncedClickableNoRipple(
                    role = Role.Button,
                    onClickLabel = stringResource(R.string.balances_metric_info_cd),
                    onClick = { onShowMetricInfo(BalanceMetricType.REMAINING) }
                ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PocketRemainingLabelRow()
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.ExtraSmall))
                AnimatedAmount(
                    formattedAmount = balance.formattedBalance,
                    shouldAnimate = shouldAnimateBalance,
                    previousAmount = previousBalance,
                    rollingUp = balanceRollingUp,
                    style = amountStyle,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    onAnimationComplete = onBalanceAnimationComplete
                )
            }
            if (balance.formattedAvailableBalance != null ||
                balance.formattedScheduledHoldAmount != null ||
                balance.formattedRefundableHoldAmount != null
            ) {
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.Small))
                SecondaryBalancesRow(
                    balance = balance,
                    onShowMetricInfo = onShowMetricInfo
                )
            }
        }
    }
}
