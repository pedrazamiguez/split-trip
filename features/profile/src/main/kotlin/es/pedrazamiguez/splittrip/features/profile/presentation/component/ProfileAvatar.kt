package es.pedrazamiguez.splittrip.features.profile.presentation.component

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
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
import es.pedrazamiguez.splittrip.features.profile.R

private val AVATAR_INITIAL_SPLIT_REGEX = Regex("[\\s._-]+")
private const val MAX_INITIAL_CHARS = 2
private const val NON_PRO_BORDER_ALPHA = 0.18f
private val AVATAR_SIZE = 120.dp

@Composable
internal fun ProfileAvatar(
    profileImageUrl: String?,
    isPro: Boolean,
    modifier: Modifier = Modifier,
    displayName: String = ""
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
        Modifier.border(
            width = 2.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = NON_PRO_BORDER_ALPHA),
            shape = CircleShape
        )
    }
    val pictureDescription = stringResource(R.string.profile_picture_description)

    Box(
        modifier = modifier.size(AVATAR_SIZE),
        contentAlignment = Alignment.Center
    ) {
        if (!profileImageUrl.isNullOrBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(profileImageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = pictureDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(AVATAR_SIZE)
                    .clip(CircleShape)
                    .then(borderModifier)
            )
        } else {
            ProfileAvatarFallback(
                displayName = displayName,
                pictureDescription = pictureDescription,
                borderModifier = borderModifier
            )
        }

        if (isPro) {
            ProfileAvatarBadge(modifier = Modifier.align(Alignment.BottomEnd))
        }
    }
}

internal fun extractAvatarInitials(displayName: String): String {
    val trimmed = displayName.trim()
    if (trimmed.isBlank()) return ""
    val localPart = trimmed.substringBefore('@')
    return localPart
        .split(AVATAR_INITIAL_SPLIT_REGEX)
        .filter { it.isNotBlank() }
        .take(MAX_INITIAL_CHARS)
        .joinToString("") { token -> token.first().uppercaseChar().toString() }
}
