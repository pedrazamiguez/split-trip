package es.pedrazamiguez.splittrip.features.group.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Photo
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip.PassportTag
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip.PassportTagStyle
import es.pedrazamiguez.splittrip.domain.enums.GroupStatus
import es.pedrazamiguez.splittrip.features.group.R

private val COVER_IMAGE_HEIGHT = 160.dp

@Suppress("LongMethod")
@Composable
internal fun SelectedGroupCoverImage(
    imageUrl: String?,
    groupName: String,
    status: GroupStatus = GroupStatus.ACTIVE,
    showBadge: Boolean = true,
    modifier: Modifier = Modifier
) {
    var imageLoadFailed by remember(imageUrl) { mutableStateOf(false) }
    Box(
        modifier = modifier
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
            val gradientStart = MaterialTheme.colorScheme.primary
            val gradientEnd = MaterialTheme.colorScheme.secondary
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(COVER_IMAGE_HEIGHT)
                    .background(
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
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)
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
        if (showBadge) {
            when (status) {
                GroupStatus.ACTIVE -> {
                    PassportTag(
                        text = stringResource(R.string.group_active_badge),
                        style = PassportTagStyle.SUCCESS,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(MaterialTheme.spacing.Default)
                    )
                }
                GroupStatus.ARCHIVED -> {
                    GroupArchivedTag(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(MaterialTheme.spacing.Default)
                    )
                }
            }
        }
    }
}
