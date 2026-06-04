package es.pedrazamiguez.splittrip.features.group.presentation.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import es.pedrazamiguez.splittrip.features.group.R
import kotlinx.collections.immutable.ImmutableList

private val AVATAR_SIZE = 36.dp
private val AVATAR_OVERLAP_OFFSET = 20.dp

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
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(url).crossfade(true).build(),
                contentDescription = stringResource(R.string.group_member_avatar_description),
                contentScale = ContentScale.Crop,
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
