package es.pedrazamiguez.splittrip.features.settlement.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalBottomPadding
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalTopPadding
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.InlineWarningBanner
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.sheet.CashBreakdownBottomSheet
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.CaptionText
import es.pedrazamiguez.splittrip.features.settlement.R
import es.pedrazamiguez.splittrip.features.settlement.presentation.model.MemberSpendingChartUiModel
import es.pedrazamiguez.splittrip.features.settlement.presentation.model.PersonalPositionUiModel
import es.pedrazamiguez.splittrip.features.settlement.presentation.model.SettlementConsensusItemUiModel
import es.pedrazamiguez.splittrip.features.settlement.presentation.viewmodel.event.YourBalanceUiEvent
import kotlinx.collections.immutable.ImmutableList

@Suppress("LongMethod")
@Composable
internal fun YourBalanceContent(
    personalPosition: PersonalPositionUiModel,
    isCashBreakdownVisible: Boolean,
    settlementConsensus: ImmutableList<SettlementConsensusItemUiModel>,
    chart: MemberSpendingChartUiModel?,
    isCashOnly: Boolean,
    onEvent: (YourBalanceUiEvent) -> Unit,
    modifier: Modifier = Modifier,
    isOffline: Boolean = false
) {
    val bottomPadding = LocalBottomPadding.current
    val topPadding = LocalTopPadding.current
    val spacing = MaterialTheme.spacing

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = spacing.Default,
            end = spacing.Default,
            top = spacing.Default + topPadding,
            bottom = bottomPadding + spacing.Default
        ),
        verticalArrangement = Arrangement.spacedBy(spacing.Medium)
    ) {
        if (isOffline) {
            item(key = "offline_warning_banner") {
                InlineWarningBanner(warning = UiText.StringResource(R.string.your_balance_offline_warning))
            }
        }
        item(key = "hero_net_position") {
            YourBalanceHeroBanner(personalPosition = personalPosition)
        }
        item(key = "pocket_cash_row") {
            YourBalancePocketCashRow(
                personalPosition = personalPosition,
                onShowCashBreakdown = { onEvent(YourBalanceUiEvent.ShowCashBreakdown) }
            )
        }
        item(key = "spending_chart") {
            chart?.let {
                MemberSpendingBarChart(
                    chart = it,
                    isCashOnly = isCashOnly,
                    onToggle = { cashOnly -> onEvent(YourBalanceUiEvent.ChartModeToggled(cashOnly)) }
                )
            }
        }
        item(key = "activity_breakdown") {
            YourBalanceActivityBreakdown(personalPosition = personalPosition)
        }
        if (personalPosition.hasNegativeCashInHand) {
            item(key = "negative_cash_hint") {
                CaptionText(
                    text = stringResource(R.string.your_balance_negative_cash_hint),
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
        item(key = "settlement_consensus") {
            SettlementConsensusSection(
                settlements = settlementConsensus,
                isOffline = isOffline,
                onConfirm = { onEvent(YourBalanceUiEvent.ConfirmSettlement(it)) },
                onDispute = { onEvent(YourBalanceUiEvent.DisputeSettlement(it)) },
                onNudge = { onEvent(YourBalanceUiEvent.NudgeDebtor(it)) }
            )
        }
    }

    if (isCashBreakdownVisible) {
        val breakdownItems = personalPosition.cashBreakdown
        CashBreakdownBottomSheet(
            memberName = stringResource(R.string.your_balance_title),
            breakdown = breakdownItems,
            formattedTotal = personalPosition.formattedCashInHand,
            formattedTotalFees = personalPosition.formattedTotalFees ?: "",
            onDismiss = { onEvent(YourBalanceUiEvent.DismissCashBreakdown) }
        )
    }
}
