package es.pedrazamiguez.splittrip.features.profile.presentation.component

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.BodyText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.ScreenTitleText

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
        isPro = isPro,
        displayName = displayName
    )
    Spacer(modifier = Modifier.height(MaterialTheme.spacing.ExtraLarge))
    ScreenTitleText(
        text = displayName,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(MaterialTheme.spacing.ExtraSmall))
    BodyText(
        text = email,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )
}
