package es.pedrazamiguez.splittrip.features.group.presentation.component

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.AlignJustified
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.FlatCard
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.SyncStatusBadge
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.SecondaryBodyText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.SheetTitleText
import es.pedrazamiguez.splittrip.features.group.presentation.model.GroupUiModel

private val CURRENCY_HORIZONTAL_PADDING = 14.dp
private val CURRENCY_VERTICAL_PADDING = 8.dp
private val CARD_SHADOW_ELEVATION = 8.dp

@Suppress("LongMethod", "CognitiveComplexMethod")
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SelectedGroupCard(
    groupUiModel: GroupUiModel,
    modifier: Modifier = Modifier,
    onClick: (groupId: String, groupName: String, currency: String) -> Unit = { _, _, _ -> },
    onLongClick: () -> Unit = {}
) {
    val haptics = LocalHapticFeedback.current
    val cardShape = MaterialTheme.shapes.large

    Box(modifier = modifier) {
        FlatCard(
            modifier = Modifier
                .fillMaxWidth()
                .clip(cardShape)
                .combinedClickable(
                    onClick = { onClick(groupUiModel.id, groupUiModel.name, groupUiModel.currency) },
                    onLongClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLongClick()
                    }
                ),
            elevation = CARD_SHADOW_ELEVATION
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                SelectedGroupCoverImage(
                    imageUrl = groupUiModel.imageUrl,
                    groupName = groupUiModel.name,
                    showActiveBadge = true
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(MaterialTheme.spacing.Default),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)
                ) {
                    // Name row with currency chip
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SheetTitleText(
                            text = groupUiModel.name,
                            maxLines = 1,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = MaterialTheme.spacing.Medium)
                        )
                        Box(
                            modifier = Modifier
                                .clip(MaterialTheme.shapes.large)
                                .background(MaterialTheme.colorScheme.primary)
                        ) {
                            Text(
                                text = groupUiModel.currency,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(
                                    horizontal = CURRENCY_HORIZONTAL_PADDING,
                                    vertical = CURRENCY_VERTICAL_PADDING
                                )
                            )
                        }
                    }

                    // Description row
                    if (groupUiModel.description.isNotEmpty()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.ExtraSmall),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = TablerIcons.Outline.AlignJustified,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            SecondaryBodyText(text = groupUiModel.description)
                        }
                    }

                    // Avatar stack + member count
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)
                    ) {
                        if (groupUiModel.memberAvatarUrls.isNotEmpty() || groupUiModel.memberOverflowCount > 0) {
                            MemberAvatarStack(
                                avatarUrls = groupUiModel.memberAvatarUrls,
                                overflowCount = groupUiModel.memberOverflowCount
                            )
                        }
                        if (groupUiModel.membersCountText.isNotEmpty()) {
                            SecondaryBodyText(
                                text = groupUiModel.membersCountText,
                                maxLines = Int.MAX_VALUE
                            )
                        }
                    }
                }
            }
        }
        SyncStatusBadge(syncStatus = groupUiModel.syncStatus)
    }
}
