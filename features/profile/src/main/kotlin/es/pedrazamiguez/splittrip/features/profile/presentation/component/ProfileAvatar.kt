package es.pedrazamiguez.splittrip.features.profile.presentation.component

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import es.pedrazamiguez.splittrip.core.designsystem.icon.filled.UserFilled
import es.pedrazamiguez.splittrip.features.profile.R

@Composable
internal fun ProfileAvatar(
    profileImageUrl: String?,
    isPro: Boolean,
    modifier: Modifier = Modifier
) {
    val borderModifier = if (isPro) {
        Modifier.border(
            width = 2.5.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    MaterialTheme.colorScheme.tertiaryContainer,
                    MaterialTheme.colorScheme.tertiary
                )
            ),
            shape = CircleShape
        )
    } else {
        Modifier
    }

    Box(
        modifier = modifier.size(120.dp),
        contentAlignment = Alignment.Center
    ) {
        if (profileImageUrl != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(profileImageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = stringResource(R.string.profile_picture_description),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .then(borderModifier)
            )
        } else {
            Surface(
                modifier = Modifier
                    .size(120.dp)
                    .then(borderModifier),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(
                    imageVector = TablerIcons.Filled.UserFilled,
                    contentDescription = stringResource(R.string.profile_picture_description),
                    modifier = Modifier.padding(MaterialTheme.spacing.ExtraLarge),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        if (isPro) {
            ProfileAvatarBadge(
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }
    }
}
