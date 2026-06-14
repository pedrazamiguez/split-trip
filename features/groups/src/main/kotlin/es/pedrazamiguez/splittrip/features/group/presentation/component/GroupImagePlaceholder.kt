package es.pedrazamiguez.splittrip.features.group.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Photo
import es.pedrazamiguez.splittrip.features.group.R

@Composable
internal fun GroupImagePlaceholder(
    coverHeight: Dp,
    modifier: Modifier = Modifier
) {
    val gradientStart = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
    val gradientEnd = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(coverHeight)
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
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = stringResource(R.string.group_cover_photo_placeholder),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
            )
        }
    }
}
