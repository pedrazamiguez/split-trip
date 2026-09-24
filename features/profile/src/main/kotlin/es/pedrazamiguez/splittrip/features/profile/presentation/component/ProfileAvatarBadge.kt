package es.pedrazamiguez.splittrip.features.profile.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import es.pedrazamiguez.splittrip.core.designsystem.R as CoreDesignR
import es.pedrazamiguez.splittrip.core.designsystem.presentation.topbar.PROFILE_AVATAR_PRO_BADGE_TEST_TAG

@Composable
internal fun ProfileAvatarBadge(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.tertiary)
            .border(
                width = 2.dp,
                color = MaterialTheme.colorScheme.surface,
                shape = CircleShape
            )
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .testTag(PROFILE_AVATAR_PRO_BADGE_TEST_TAG),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(CoreDesignR.string.badge_pro),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 12.sp
            ),
            color = MaterialTheme.colorScheme.onTertiary
        )
    }
}
