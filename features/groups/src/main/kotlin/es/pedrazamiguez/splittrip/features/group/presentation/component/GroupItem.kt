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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import es.pedrazamiguez.splittrip.core.designsystem.R as DesignR
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.ChevronRight
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Photo
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.FlatCard
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.SyncStatusBadge
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.SecondaryBodyText
import es.pedrazamiguez.splittrip.features.group.presentation.model.GroupUiModel

private val THUMBNAIL_SIZE = 56.dp
private val CURRENCY_HORIZONTAL_PADDING = 10.dp
private val CURRENCY_VERTICAL_PADDING = 5.dp

/**
 * Compact horizontal card for an **unselected** group in the groups list.
 *
 * Layout:
 * ```
 * ┌──────────────────────────────────────────────┐
 * │  [56dp thumbnail]  Group Name       [EUR]  › │
 * │                    📅 1 jan  ·  2 travelers   │
 * └──────────────────────────────────────────────┘
 * ```
 *
 * Selected groups are rendered by [SelectedGroupCard] instead.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GroupItem(
    groupUiModel: GroupUiModel,
    modifier: Modifier = Modifier,
    onClick: (groupId: String, groupName: String, currency: String) -> Unit = { _, _, _ -> },
    onLongClick: () -> Unit = {}
) {
    val haptics = LocalHapticFeedback.current

    Box(modifier = modifier) {
        FlatCard(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .combinedClickable(onClick = {
                    onClick(groupUiModel.id, groupUiModel.name, groupUiModel.currency)
                }, onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                })
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(MaterialTheme.spacing.Medium),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GroupThumbnail(imageUrl = groupUiModel.imageUrl)

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = groupUiModel.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    GroupItemMetaLine(groupUiModel = groupUiModel)
                }

                GroupItemTrailing(currency = groupUiModel.currency)
            }
        }
        SyncStatusBadge(syncStatus = groupUiModel.syncStatus)
    }
}

@Composable
private fun GroupThumbnail(imageUrl: String?, modifier: Modifier = Modifier) {
    val shape = MaterialTheme.shapes.medium
    if (imageUrl != null) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current).data(imageUrl).crossfade(true).build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(THUMBNAIL_SIZE)
                .clip(shape)
        )
    } else {
        Box(
            modifier = modifier
                .size(THUMBNAIL_SIZE)
                .clip(shape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = TablerIcons.Outline.Photo,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun GroupItemMetaLine(groupUiModel: GroupUiModel) {
    val metaParts = buildList {
        if (groupUiModel.dateText.isNotEmpty()) add(groupUiModel.dateText)
        if (groupUiModel.membersCountText.isNotEmpty()) add(groupUiModel.membersCountText)
    }
    if (metaParts.isEmpty()) return

    Row(
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.ExtraSmall),
        verticalAlignment = Alignment.CenterVertically
    ) {
        metaParts.forEachIndexed { index, part ->
            if (index > 0) {
                SecondaryBodyText(
                    text = stringResource(DesignR.string.metadata_separator),
                    maxLines = Int.MAX_VALUE
                )
            }
            SecondaryBodyText(text = part)
        }
    }
}

@Composable
private fun GroupItemTrailing(currency: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.ExtraSmall)
    ) {
        Box(
            modifier = Modifier
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.primaryContainer)
        ) {
            Text(
                text = currency,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(
                    horizontal = CURRENCY_HORIZONTAL_PADDING,
                    vertical = CURRENCY_VERTICAL_PADDING
                )
            )
        }
        Icon(
            imageVector = TablerIcons.Outline.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}
