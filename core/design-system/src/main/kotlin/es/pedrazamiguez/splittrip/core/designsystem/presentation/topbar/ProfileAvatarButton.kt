package es.pedrazamiguez.splittrip.core.designsystem.presentation.topbar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import es.pedrazamiguez.splittrip.core.designsystem.R
import es.pedrazamiguez.splittrip.core.designsystem.extension.debouncedClickable
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.filled.UserFilled

val LocalProfileAvatarUrl = compositionLocalOf<String?> { null }
val LocalIsProUser = compositionLocalOf { false }

const val PROFILE_AVATAR_BUTTON_TEST_TAG = "ProfileAvatarButton"
const val PROFILE_AVATAR_PRO_BADGE_TEST_TAG = "ProfileAvatarProBadge"

@Composable
fun ProfileAvatarButton(
    avatarUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPro: Boolean = LocalIsProUser.current
) {
    val avatarBorderModifier = if (isPro) {
        Modifier.border(
            width = 1.5.dp,
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
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .debouncedClickable(onClick = onClick)
            .testTag(PROFILE_AVATAR_BUTTON_TEST_TAG),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier.size(32.dp),
            contentAlignment = Alignment.Center
        ) {
            AvatarContent(
                avatarUrl = avatarUrl,
                borderModifier = avatarBorderModifier
            )

            if (isPro) {
                ProMicroBadge(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .testTag(PROFILE_AVATAR_PRO_BADGE_TEST_TAG)
                )
            }
        }
    }
}

@Composable
private fun AvatarContent(
    avatarUrl: String?,
    borderModifier: Modifier
) {
    if (!avatarUrl.isNullOrBlank()) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(avatarUrl)
                .crossfade(true)
                .build(),
            contentDescription = stringResource(R.string.content_description_profile),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .then(borderModifier)
        )
    } else {
        Surface(
            modifier = Modifier
                .size(32.dp)
                .then(borderModifier),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Icon(
                imageVector = TablerIcons.Filled.UserFilled,
                contentDescription = stringResource(R.string.content_description_profile),
                modifier = Modifier.padding(6.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun ProMicroBadge(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.tertiary)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.surface,
                shape = CircleShape
            )
            .padding(horizontal = 3.dp, vertical = 0.5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.badge_pro),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 7.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 8.sp
            ),
            color = MaterialTheme.colorScheme.onTertiary
        )
    }
}
