package es.pedrazamiguez.splittrip.features.group.presentation.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import es.pedrazamiguez.splittrip.core.designsystem.extension.debouncedCombinedClickable
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
 * Selected groups are rendered by [SelectedGroupCard] instead.
 */
@Suppress("LongMethod", "CognitiveComplexMethod")
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GroupItem(
    groupUiModel: GroupUiModel,
    modifier: Modifier = Modifier,
    innerModifier: Modifier = Modifier,
    onClick: (groupId: String, groupName: String, currency: String) -> Unit = { _, _, _ -> },
    onLongClick: () -> Unit = {}
) {
    val haptics = LocalHapticFeedback.current

    Box(modifier = modifier) {
        FlatCard(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .then(innerModifier)
                .debouncedCombinedClickable(
                    enableSpringPress = true,
                    onClick = {
                        onClick(groupUiModel.id, groupUiModel.name, groupUiModel.currency)
                    },
                    onLongClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLongClick()
                    }
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(MaterialTheme.spacing.Medium),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // GroupThumbnail
                val shape = MaterialTheme.shapes.medium
                if (groupUiModel.imageUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(
                            LocalContext.current
                        ).data(groupUiModel.imageUrl).crossfade(true).build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(THUMBNAIL_SIZE)
                            .clip(shape)
                    )
                } else {
                    Box(
                        modifier = Modifier
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

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Line 1: Title & Primary Currency Badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = groupUiModel.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = MaterialTheme.spacing.Small)
                        )
                        Box(
                            modifier = Modifier
                                .clip(MaterialTheme.shapes.large)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Text(
                                text = groupUiModel.currency,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(
                                    horizontal = CURRENCY_HORIZONTAL_PADDING,
                                    vertical = CURRENCY_VERTICAL_PADDING
                                )
                            )
                        }
                    }

                    // Line 2: Meta & Secondary Currency Chips
                    val metaParts = buildList {
                        if (groupUiModel.dateText.isNotEmpty()) add(groupUiModel.dateText)
                        if (groupUiModel.membersCountText.isNotEmpty()) add(groupUiModel.membersCountText)
                    }
                    if (metaParts.isNotEmpty() || groupUiModel.extraCurrencies.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (metaParts.isNotEmpty()) {
                                Arrangement.SpaceBetween
                            } else {
                                Arrangement.End
                            },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (metaParts.isNotEmpty()) {
                                Row(
                                    modifier = Modifier
                                        .weight(1f, fill = false)
                                        .padding(end = MaterialTheme.spacing.Small),
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
                            if (groupUiModel.extraCurrencies.isNotEmpty()) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.ExtraSmall),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    groupUiModel.extraCurrencies.forEach { extraCurrency ->
                                        GroupExtraCurrencyChip(currency = extraCurrency)
                                    }
                                    groupUiModel.extraCurrenciesOverflowText?.let { overflowText ->
                                        GroupExtraCurrencyChip(currency = overflowText)
                                    }
                                }
                            }
                        }
                    }
                }

                Icon(
                    imageVector = TablerIcons.Outline.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
        SyncStatusBadge(syncStatus = groupUiModel.syncStatus)
    }
}
