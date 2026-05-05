package es.pedrazamiguez.splittrip.features.group.presentation.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
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
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.AlignJustified
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Photo
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.FlatCard
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.SyncStatusBadge
import es.pedrazamiguez.splittrip.features.group.R
import es.pedrazamiguez.splittrip.features.group.presentation.model.GroupUiModel
import kotlinx.collections.immutable.ImmutableList

private val COVER_IMAGE_HEIGHT = 160.dp
private val AVATAR_SIZE = 36.dp
private val AVATAR_OVERLAP_OFFSET = 20.dp
private val CURRENCY_HORIZONTAL_PADDING = 14.dp
private val CURRENCY_VERTICAL_PADDING = 8.dp
private val CARD_SHADOW_ELEVATION = 8.dp

/**
 * Hero card for the currently selected/active group.
 *
 * Renders a large cover image (or neutral placeholder with photo icon), an "Active Now" badge,
 * member avatar stack with overflow count, and richer metadata compared to the
 * compact [GroupItem] used for unselected groups.
 *
 * The card uses a neutral surface background with ambient shadow elevation so it
 * visually floats above the list. The outer [Box] is intentionally unclipped so
 * the [SyncStatusBadge] can extend slightly outside the card bounds.
 *
 * The shadow is rendered by [FlatCard] via `graphicsLayer { shape = shapes.large; clip = false }`,
 * which keeps it rounded at all frames (Horizon Narrative §4.4). Dark mode suppresses the shadow
 * automatically inside [FlatCard]. The `LazyColumn` item must pass
 * `fadeInSpec = null, fadeOutSpec = null` to `animateItem()` to prevent the alpha-compositing
 * offscreen buffer from clipping the shadow to a rectangle — see `GroupsScreen`.
 */
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

    // Outer Box is unclipped — lets SyncStatusBadge overflow beyond card bounds.
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
                    groupName = groupUiModel.name
                )
                SelectedGroupCardContent(groupUiModel = groupUiModel)
            }
        }
        SyncStatusBadge(syncStatus = groupUiModel.syncStatus)
    }
}

// FlatCard's Surface clips all content to shapes.large — no extra clip needed here.
@Composable
internal fun SelectedGroupCoverImage(
    imageUrl: String?,
    groupName: String,
    showActiveBadge: Boolean = true
) {
    // Also tracks Coil load failures — shows placeholder if the URL is valid but unreachable.
    var imageLoadFailed by remember(imageUrl) { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(COVER_IMAGE_HEIGHT)
    ) {
        if (imageUrl != null && !imageLoadFailed) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(imageUrl).crossfade(true).build(),
                contentDescription = stringResource(
                    R.string.group_cover_image_description,
                    groupName
                ),
                contentScale = ContentScale.Crop,
                onError = { imageLoadFailed = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(COVER_IMAGE_HEIGHT)
            )
        } else {
            GroupCoverImagePlaceholder(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(COVER_IMAGE_HEIGHT)
            )
        }
        if (showActiveBadge) {
            ActiveNowBadge(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            )
        }
    }
}

/**
 * Gradient placeholder shown when the group has no cover image yet.
 *
 * Uses the app's brand gradient (primary → secondary) so it reads as a deliberate
 * design choice rather than a missing asset. The Photo icon reinforces the purpose
 * of the space; a "Add cover photo" label will be added once the upload flow is built.
 *
 * Replace the placeholder illustration asset with the final branded artwork when it becomes available.
 */
@Composable
private fun GroupCoverImagePlaceholder(modifier: Modifier = Modifier) {
    val gradientStart = MaterialTheme.colorScheme.primary
    val gradientEnd = MaterialTheme.colorScheme.secondary
    Box(
        modifier = modifier.background(
            brush = Brush.linearGradient(
                colors = listOf(gradientStart, gradientEnd),
                start = Offset.Zero,
                end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
            )
        ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = TablerIcons.Outline.Photo,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f),
                modifier = Modifier.size(40.dp)
            )
            Text(
                text = stringResource(R.string.group_cover_photo_placeholder),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f)
            )
        }
    }
}

@Composable
private fun ActiveNowBadge(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Text(
            text = stringResource(R.string.group_active_badge),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Suppress("LongMethod")
@Composable
private fun SelectedGroupCardContent(groupUiModel: GroupUiModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Name row with currency chip
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = groupUiModel.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            // Primary color chip — high contrast on surfaceContainerLow card background.
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.primary
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
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = TablerIcons.Outline.AlignJustified,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = groupUiModel.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Avatar stack + member count
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (groupUiModel.memberAvatarUrls.isNotEmpty() || groupUiModel.memberOverflowCount > 0) {
                MemberAvatarStack(
                    avatarUrls = groupUiModel.memberAvatarUrls,
                    overflowCount = groupUiModel.memberOverflowCount
                )
            }
            if (groupUiModel.membersCountText.isNotEmpty()) {
                Text(
                    text = groupUiModel.membersCountText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Renders a horizontal stack of overlapping member avatar circles with an optional overflow badge.
 *
 * Each circle shows a profile image loaded via Coil when a URL is available.
 * An overflow circle "+N" is appended when [overflowCount] > 0.
 */
@Composable
internal fun MemberAvatarStack(
    avatarUrls: ImmutableList<String>,
    overflowCount: Int,
    modifier: Modifier = Modifier
) {
    val totalCircles = avatarUrls.size + if (overflowCount > 0) 1 else 0
    val totalWidth = AVATAR_SIZE + AVATAR_OVERLAP_OFFSET * (totalCircles - 1).coerceAtLeast(0)

    Box(
        modifier = modifier
            .width(totalWidth)
            .height(AVATAR_SIZE)
    ) {
        avatarUrls.forEachIndexed { index, url ->
            SingleMemberAvatar(
                avatarUrl = url,
                modifier = Modifier
                    .size(AVATAR_SIZE)
                    .offset(x = AVATAR_OVERLAP_OFFSET * index)
                    .clip(CircleShape)
            )
        }
        if (overflowCount > 0) {
            val overflowIndex = avatarUrls.size
            Surface(
                modifier = Modifier
                    .size(AVATAR_SIZE)
                    .offset(x = AVATAR_OVERLAP_OFFSET * overflowIndex),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.group_member_avatar_overflow, overflowCount),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SingleMemberAvatar(avatarUrl: String, modifier: Modifier = Modifier) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current).data(avatarUrl).crossfade(true).build(),
        contentDescription = stringResource(R.string.group_member_avatar_description),
        contentScale = ContentScale.Crop,
        modifier = modifier
    )
}
