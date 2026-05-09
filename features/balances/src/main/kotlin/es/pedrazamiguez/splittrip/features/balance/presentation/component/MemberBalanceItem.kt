package es.pedrazamiguez.splittrip.features.balance.presentation.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.BuildingBank
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Cash
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.ChartBar
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.ChevronDown
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.ChevronUp
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.InfoCircle
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.User
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.FlatCard
import es.pedrazamiguez.splittrip.features.balance.R
import es.pedrazamiguez.splittrip.features.balance.presentation.model.CurrencyBreakdownUiModel
import es.pedrazamiguez.splittrip.features.balance.presentation.model.MemberBalanceUiModel
import kotlinx.collections.immutable.ImmutableList

@Composable
fun MemberBalanceItem(memberBalance: MemberBalanceUiModel, modifier: Modifier = Modifier) {
    var isExpanded by remember { mutableStateOf(false) }
    var showCashBreakdown by remember { mutableStateOf(false) }
    val balanceColor = resolveBalanceColor(memberBalance.isPositiveBalance)
    val displayName = resolveDisplayName(memberBalance)
    val expandedStateDesc = resolveExpandedStateDesc(isExpanded)
    val toggleContentDesc = resolveToggleContentDesc(isExpanded, displayName)

    MemberBalanceCard(
        modifier = modifier,
        expandedStateDesc = expandedStateDesc,
        toggleContentDesc = toggleContentDesc,
        onToggle = { isExpanded = !isExpanded }
    ) {
        MemberBalanceSummaryRow(
            memberBalance = memberBalance,
            displayName = displayName,
            balanceColor = balanceColor,
            isExpanded = isExpanded,
            toggleContentDesc = toggleContentDesc
        )
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            MemberBalanceExpandedDetail(
                memberBalance = memberBalance,
                balanceColor = balanceColor,
                onShowCashBreakdown = { showCashBreakdown = true }
            )
        }
    }

    if (showCashBreakdown) {
        CashBreakdownBottomSheet(
            breakdown = memberBalance.cashBreakdown,
            formattedTotal = memberBalance.formattedCashInHand,
            onDismiss = { showCashBreakdown = false }
        )
    }
}

@Composable
private fun resolveBalanceColor(isPositive: Boolean) =
    if (isPositive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error

@Composable
private fun resolveDisplayName(memberBalance: MemberBalanceUiModel) =
    if (memberBalance.isCurrentUser) stringResource(R.string.balances_member_you) else memberBalance.displayName

@Composable
private fun resolveExpandedStateDesc(isExpanded: Boolean) =
    if (isExpanded) {
        stringResource(R.string.balances_member_expanded)
    } else {
        stringResource(R.string.balances_member_collapsed)
    }

@Composable
private fun resolveToggleContentDesc(isExpanded: Boolean, displayName: String) =
    if (isExpanded) {
        stringResource(R.string.balances_member_collapse, displayName)
    } else {
        stringResource(R.string.balances_member_expand, displayName)
    }

@Composable
private fun MemberBalanceCard(
    modifier: Modifier = Modifier,
    expandedStateDesc: String,
    toggleContentDesc: String,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    FlatCard(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                stateDescription = expandedStateDesc
                contentDescription = toggleContentDesc
            }
            .clickable(onClick = onToggle)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            content = { content() }
        )
    }
}

@Composable
private fun MemberBalanceSummaryRow(
    memberBalance: MemberBalanceUiModel,
    displayName: String,
    balanceColor: Color,
    isExpanded: Boolean,
    toggleContentDesc: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(imageVector = TablerIcons.Outline.User, contentDescription = null, tint = balanceColor)
        Column(modifier = Modifier.weight(1f)) {
            Text(text = displayName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BreakdownLabel(
                    label = stringResource(R.string.balances_member_contributed_label),
                    value = memberBalance.formattedContributed
                )
                BreakdownLabel(
                    label = stringResource(R.string.balances_member_cash_in_hand_label),
                    value = memberBalance.formattedCashInHand
                )
                BreakdownLabel(
                    label = stringResource(R.string.balances_member_spent_label),
                    value = memberBalance.formattedTotalSpent
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = memberBalance.formattedPocketBalance,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = balanceColor
            )
            Icon(
                imageVector = if (isExpanded) TablerIcons.Outline.ChevronUp else TablerIcons.Outline.ChevronDown,
                contentDescription = toggleContentDesc,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MemberBalanceExpandedDetail(
    memberBalance: MemberBalanceUiModel,
    balanceColor: Color,
    onShowCashBreakdown: () -> Unit
) {
    Column(modifier = Modifier.padding(top = 8.dp)) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(8.dp))

        DetailRow(
            label = stringResource(R.string.balances_member_pocket_balance),
            value = memberBalance.formattedPocketBalance,
            valueColor = balanceColor,
            icon = TablerIcons.Outline.BuildingBank
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Cash-in-hand row with info button to open the cash breakdown sheet
        CashInHandRow(
            memberBalance = memberBalance,
            onShowCashBreakdown = onShowCashBreakdown
        )

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        Spacer(modifier = Modifier.height(8.dp))

        SpendingBreakdownSection(memberBalance = memberBalance)
    }
}

@Composable
private fun CashInHandRow(
    memberBalance: MemberBalanceUiModel,
    onShowCashBreakdown: () -> Unit
) {
    DetailRow(
        label = stringResource(R.string.balances_member_cash_in_hand),
        value = memberBalance.formattedCashInHand,
        icon = TablerIcons.Outline.Cash,
        labelSuffix = if (memberBalance.cashBreakdown.isNotEmpty()) {
            {
                Icon(
                    imageVector = TablerIcons.Outline.InfoCircle,
                    contentDescription = stringResource(R.string.balances_cash_breakdown_view_cd),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier
                        .size(14.dp)
                        .clickable(onClick = onShowCashBreakdown)
                )
            }
        } else {
            null
        }
    )
    when {
        memberBalance.hasNegativeCashInHand -> {
            EmptyHintText(text = stringResource(R.string.balances_member_negative_cash_hint))
        }
        memberBalance.cashInHandByCurrency.isNotEmpty() -> {
            CurrencyBreakdownRows(items = memberBalance.cashInHandByCurrency)
        }
        else -> {
            EmptyHintText(text = stringResource(R.string.balances_member_no_cash))
        }
    }
}

@Composable
private fun SpendingBreakdownSection(memberBalance: MemberBalanceUiModel) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = TablerIcons.Outline.ChartBar,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(R.string.balances_member_spent_breakdown),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    if (memberBalance.cashSpentByCurrency.isEmpty() && memberBalance.nonCashSpentByCurrency.isEmpty()) {
        EmptyHintText(text = stringResource(R.string.balances_member_no_expenses))
    } else {
        Spacer(modifier = Modifier.height(4.dp))
        if (memberBalance.cashSpentByCurrency.isNotEmpty()) {
            DetailRow(
                label = stringResource(R.string.balances_member_cash_expenses),
                value = memberBalance.formattedCashSpent
            )
            CurrencyBreakdownRows(items = memberBalance.cashSpentByCurrency)
        }
        if (memberBalance.nonCashSpentByCurrency.isNotEmpty()) {
            DetailRow(
                label = stringResource(R.string.balances_member_non_cash_expenses),
                value = memberBalance.formattedNonCashSpent
            )
            CurrencyBreakdownRows(items = memberBalance.nonCashSpentByCurrency)
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    icon: ImageVector? = null,
    labelSuffix: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            // Optional composable placed inline after the label (e.g. an info icon)
            labelSuffix?.invoke()
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = valueColor
        )
    }
}

@Composable
private fun CurrencyBreakdownRows(items: ImmutableList<CurrencyBreakdownUiModel>) {
    items.forEach { item ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, top = 2.dp, bottom = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = item.currency,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = item.formattedAmount,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
                if (item.formattedEquivalent.isNotBlank()) {
                    Text(
                        text = item.formattedEquivalent,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyHintText(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(start = 20.dp, top = 4.dp),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    )
}

@Composable
private fun BreakdownLabel(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
    }
}
