package es.pedrazamiguez.splittrip.features.profile.presentation.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.filled.UserFilled
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.ScreenTitleText

private val AVATAR_SIZE = 120.dp

@Composable
internal fun ProfileAvatarFallback(
    displayName: String,
    pictureDescription: String,
    borderModifier: Modifier,
    modifier: Modifier = Modifier
) {
    val initials = remember(displayName) { extractAvatarInitials(displayName) }
    val surfaceSemantics = if (initials.isNotEmpty()) {
        Modifier.semantics { contentDescription = pictureDescription }
    } else {
        Modifier
    }

    Surface(
        modifier = modifier
            .size(AVATAR_SIZE)
            .then(borderModifier)
            .then(surfaceSemantics),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (initials.isNotEmpty()) {
                ScreenTitleText(
                    text = initials,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center
                )
            } else {
                Icon(
                    imageVector = TablerIcons.Filled.UserFilled,
                    contentDescription = pictureDescription,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(MaterialTheme.spacing.ExtraLarge),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}
