package es.pedrazamiguez.splittrip.features.group.presentation.component.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.filled.UserFilled
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.BodyText
import es.pedrazamiguez.splittrip.features.group.R
import es.pedrazamiguez.splittrip.features.group.presentation.model.GroupMemberUiModel

private val AVATAR_SIZE = 36.dp

@Suppress("LongMethod")
@Composable
fun GroupMemberItem(
    member: GroupMemberUiModel,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (member.avatarUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(member.avatarUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = stringResource(R.string.group_member_avatar_description),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(AVATAR_SIZE)
                        .clip(CircleShape)
                )
            } else {
                Surface(
                    modifier = Modifier.size(AVATAR_SIZE),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = TablerIcons.Filled.UserFilled,
                            contentDescription = stringResource(R.string.group_member_avatar_description),
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            BodyText(
                text = member.displayName,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }

        if (member.roleBadgeText.isNotBlank()) {
            val containerColor = if (member.isCreator) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerHighest
            }
            val contentColor = if (member.isCreator) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
            Surface(
                shape = MaterialTheme.shapes.extraSmall,
                color = containerColor
            ) {
                Text(
                    text = member.roleBadgeText,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = contentColor,
                    modifier = Modifier.padding(
                        horizontal = MaterialTheme.spacing.Small,
                        vertical = 2.dp
                    )
                )
            }
        }
    }
}
