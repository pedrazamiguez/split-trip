package es.pedrazamiguez.splittrip.features.expense.presentation.component.list

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.CircleCheck
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.CirclePlus
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Clock
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Sitemap
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.User
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.UsersGroup
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.FlatCard
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.SyncStatusBadge
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.BodyText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.SecondaryBodyText
import es.pedrazamiguez.splittrip.domain.enums.ExpenseCategory
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.extensions.toIconVector
import es.pedrazamiguez.splittrip.features.expense.presentation.model.ExpenseUiModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExpenseItem(
    modifier: Modifier = Modifier,
    expenseUiModel: ExpenseUiModel,
    onClick: (String) -> Unit = { _ -> },
    onLongClick: () -> Unit = {}
) {
    val haptics = LocalHapticFeedback.current

    Box(modifier = modifier) {
        FlatCard(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .combinedClickable(
                    onClick = { onClick(expenseUiModel.id) },
                    onLongClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLongClick()
                    }
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(MaterialTheme.spacing.Default),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)
            ) {
                ExpenseItemTitleRow(expenseUiModel = expenseUiModel)
                ExpenseItemMetaRow(expenseUiModel = expenseUiModel)
            }
        }
        SyncStatusBadge(syncStatus = expenseUiModel.syncStatus)
    }
}

@Composable
private fun ExpenseItemTitleRow(expenseUiModel: ExpenseUiModel) {
    // ── Row 1: [category icon] | title  |  amount ────────────────────────
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CategoryIconContainer(
            category = expenseUiModel.category,
            contentDescription = expenseUiModel.categoryText.takeIf { it.isNotBlank() }
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = MaterialTheme.spacing.Medium, end = MaterialTheme.spacing.Default),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.ExtraSmall)
        ) {
            Text(
                text = expenseUiModel.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        ExpenseAmountBadges(expenseUiModel = expenseUiModel)
    }
}

@Composable
private fun CategoryIconContainer(category: ExpenseCategory, contentDescription: String?) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = category.toIconVector(),
            contentDescription = contentDescription,
            modifier = Modifier.size(22.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ExpenseAmountBadges(expenseUiModel: ExpenseUiModel) {
    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.ExtraSmall)
    ) {
        Box(
            modifier = Modifier
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.tertiaryContainer)
        ) {
            Text(
                text = expenseUiModel.formattedAmount,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = MaterialTheme.spacing.Small)
            )
        }
        if (expenseUiModel.formattedOriginalAmount != null) {
            Box(
                modifier = Modifier
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text(
                    text = expenseUiModel.formattedOriginalAmount,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }
    }
}

@Composable
private fun ExpenseItemMetaRow(expenseUiModel: ExpenseUiModel) {
    // ── Row 2: payment method · add-on badge  |  scheduled badge ──
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Weight is applied here, inside RowScope, where the extension is available.
        val badgesModifier = if (expenseUiModel.scheduledBadgeText != null) {
            Modifier.weight(1f)
        } else {
            Modifier
        }
        ExpenseItemBadgesRow(expenseUiModel = expenseUiModel, modifier = badgesModifier)

        if (expenseUiModel.scheduledBadgeText != null) {
            ScheduledBadge(
                badgeText = expenseUiModel.scheduledBadgeText,
                isPastDue = expenseUiModel.isScheduledPastDue
            )
        }
    }
}

@Composable
private fun ExpenseItemBadgesRow(expenseUiModel: ExpenseUiModel, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (expenseUiModel.paymentMethodText.isNotEmpty()) {
            BodyText(
                text = expenseUiModel.paymentMethodText,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
        if (expenseUiModel.hasAddOns) {
            AddOnBadge()
        }
        if (expenseUiModel.isOutOfPocket && expenseUiModel.fundingSourceText != null) {
            OutOfPocketBadge(
                fundingSourceText = expenseUiModel.fundingSourceText,
                isSubunitScope = expenseUiModel.isSubunitScope,
                isGroupScope = expenseUiModel.isGroupScope
            )
        }
    }
}

@Composable
private fun AddOnBadge() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = TablerIcons.Outline.CirclePlus,
            contentDescription = stringResource(R.string.expense_has_add_ons),
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        SecondaryBodyText(
            text = stringResource(R.string.expense_add_ons_label),
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun OutOfPocketBadge(
    fundingSourceText: String,
    isSubunitScope: Boolean = false,
    isGroupScope: Boolean = false
) {
    val icon = when {
        isSubunitScope -> TablerIcons.Outline.Sitemap
        isGroupScope -> TablerIcons.Outline.UsersGroup
        else -> TablerIcons.Outline.User
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = stringResource(R.string.expense_out_of_pocket),
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        SecondaryBodyText(
            text = fundingSourceText,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun ScheduledBadge(badgeText: String, isPastDue: Boolean) {
    val tintColor = if (isPastDue) {
        MaterialTheme.colorScheme.tertiary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.ExtraSmall),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isPastDue) TablerIcons.Outline.CircleCheck else TablerIcons.Outline.Clock,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = tintColor
        )
        Text(
            text = badgeText,
            style = MaterialTheme.typography.bodyMedium,
            color = tintColor
        )
    }
}
