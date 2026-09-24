package es.pedrazamiguez.splittrip.features.profile.presentation.component

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.LargeBodyText

@Composable
internal fun ProfileHeader(
    displayName: String,
    email: String,
    profileImageUrl: String?,
    modifier: Modifier = Modifier,
    isPro: Boolean = false
) {
    Spacer(modifier = modifier.height(MaterialTheme.spacing.Section))
    ProfileAvatar(
        profileImageUrl = profileImageUrl,
        isPro = isPro
    )
    Spacer(modifier = Modifier.height(MaterialTheme.spacing.ExtraLarge))
    Text(
        text = displayName,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground
    )
    Spacer(modifier = Modifier.height(MaterialTheme.spacing.ExtraSmall))
    LargeBodyText(
        text = email,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
