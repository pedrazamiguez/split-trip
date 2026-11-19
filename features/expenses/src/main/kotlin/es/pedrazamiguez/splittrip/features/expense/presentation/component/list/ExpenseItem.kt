package es.pedrazamiguez.splittrip.features.expense.presentation.component.list

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.R as DesignSystemR
import es.pedrazamiguez.splittrip.core.designsystem.extension.debouncedCombinedClickable
import es.pedrazamiguez.splittrip.core.designsystem.extension.sharedTextAnimation
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.CirclePlus
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Sitemap
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.User
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.UsersGroup
import es.pedrazamiguez.splittrip.core.designsystem.navigation.SharedElementKeys
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip.PassportTag
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip.PassportTagStyle
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.icon.CategorySatelliteIcon
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.FlatCard
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.SyncStatusBadge
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.SecondaryBodyText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.model.MemberDisplay
import es.pedrazamiguez.splittrip.core.designsystem.transition.LocalAnimatedVisibilityScope
import es.pedrazamiguez.splittrip.core.designsystem.transition.LocalSharedTransitionScope
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.model.ExpenseUiModel

@Suppress("LongMethod", "CognitiveComplexMethod", "CyclomaticComplexMethod")
@OptIn(ExperimentalFoundationApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun ExpenseItem(
    modifier: Modifier = Modifier,
    innerModifier: Modifier = Modifier,
    expenseUiModel: ExpenseUiModel,
    onClick: (String) -> Unit = { _ -> },
    onLongClick: () -> Unit = {}
) {
    val haptics = LocalHapticFeedback.current
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedVisibilityScope = LocalAnimatedVisibilityScope.current
    val isFormer = expenseUiModel.creatorDisplay is MemberDisplay.Former
    val alphaVal = when {
        expenseUiModel.isCancelled -> CANCELLED_ALPHA
        isFormer -> 0.6f
        else -> 1f
    }

    Box(modifier = modifier) {
        FlatCard(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .then(innerModifier)
                .debouncedCombinedClickable(
                    enableSpringPress = true,
                    onClick = { onClick(expenseUiModel.id) },
                    onLongClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLongClick()
                    }
                )
                .alpha(alphaVal)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MaterialTheme.spacing.Medium, vertical = CARD_VERTICAL_PADDING),
                verticalArrangement = Arrangement.spacedBy(ROW_VERTICAL_GAP)
            ) {
                // ── Title Row ────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CategorySatelliteIcon(
                        category = expenseUiModel.category,
                        subcategory = expenseUiModel.subcategory,
                        contentDescription = expenseUiModel.categoryText.takeIf { it.isNotBlank() },
                        containerSize = CATEGORY_ICON_SIZE
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = MaterialTheme.spacing.Small),
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.ExtraSmall)
                    ) {
                        Text(
                            text = expenseUiModel.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            textDecoration = if (expenseUiModel.isCancelled) TextDecoration.LineThrough else null
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(AMOUNT_BADGE_GAP)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(MaterialTheme.shapes.large)
                                .background(MaterialTheme.colorScheme.tertiaryContainer)
                        ) {
                            Text(
                                text = expenseUiModel.formattedAmount,
                                modifier = Modifier
                                    .padding(
                                        horizontal = MaterialTheme.spacing.Medium,
                                        vertical = MaterialTheme.spacing.ExtraSmall
                                    )
                                    .sharedTextAnimation(
                                        key = SharedElementKeys.expenseAmount(expenseUiModel.id),
                                        sharedTransitionScope = sharedTransitionScope,
                                        animatedVisibilityScope = animatedVisibilityScope
                                    ),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                maxLines = 1,
                                softWrap = false,
                                textDecoration = if (expenseUiModel.isCancelled) TextDecoration.LineThrough else null
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
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(
                                        horizontal = MaterialTheme.spacing.Small,
                                        vertical = ORIGINAL_AMOUNT_VERTICAL_PADDING
                                    )
                                )
                            }
                        }
                    }
                }

                // ── Meta Row ────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val badgesModifier = if (expenseUiModel.badgeText != null) {
                        Modifier.weight(1f)
                    } else {
                        Modifier
                    }

                    Row(
                        modifier = badgesModifier,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (expenseUiModel.paymentMethodText.isNotEmpty()) {
                            PassportTag(
                                text = expenseUiModel.paymentMethodText,
                                icon = expenseUiModel.paymentMethodIcon,
                                style = PassportTagStyle.NEUTRAL
                            )
                        }
                        if (expenseUiModel.hasAddOns) {
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
                        if (expenseUiModel.isOutOfPocket && expenseUiModel.fundingSourceText != null) {
                            val icon = when {
                                expenseUiModel.isSubunitScope -> TablerIcons.Outline.Sitemap
                                expenseUiModel.isGroupScope -> TablerIcons.Outline.UsersGroup
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
                                    text = expenseUiModel.fundingSourceText,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (isFormer) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(start = 2.dp)
                                    ) {
                                        Text(
                                            text = stringResource(DesignSystemR.string.member_left_group_badge),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (expenseUiModel.badgeText != null && expenseUiModel.badgeIcon != null) {
                        val tintColor = if (expenseUiModel.isBadgeUrgent) {
                            MaterialTheme.colorScheme.tertiary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.ExtraSmall),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = expenseUiModel.badgeIcon,
                                contentDescription = null,
                                modifier = Modifier.size(STATUS_BADGE_ICON_SIZE),
                                tint = tintColor
                            )
                            Text(
                                text = expenseUiModel.badgeText,
                                style = MaterialTheme.typography.labelSmall,
                                color = tintColor,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }
        SyncStatusBadge(syncStatus = expenseUiModel.syncStatus)
    }
}

private val CARD_VERTICAL_PADDING = 10.dp
private val ROW_VERTICAL_GAP = 6.dp
private val CATEGORY_ICON_SIZE = 38.dp
private val AMOUNT_BADGE_GAP = 2.dp
private val ORIGINAL_AMOUNT_VERTICAL_PADDING = 2.dp
private val STATUS_BADGE_ICON_SIZE = 13.dp
private const val CANCELLED_ALPHA = 0.5f
