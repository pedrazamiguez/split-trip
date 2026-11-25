package es.pedrazamiguez.splittrip.core.designsystem.presentation.component.badge

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import es.pedrazamiguez.splittrip.core.designsystem.R

/**
 * Reusable Pro badge pill component indicating a gated or premium tier feature.
 */
@Composable
fun ProBadge(
    modifier: Modifier = Modifier,
    size: ProBadgeSize = ProBadgeSize.COMPACT
) {
    val horizontalPadding = when (size) {
        ProBadgeSize.COMPACT -> 4.dp
        ProBadgeSize.STANDARD -> 10.dp
    }
    val verticalPadding = when (size) {
        ProBadgeSize.COMPACT -> 1.dp
        ProBadgeSize.STANDARD -> 4.dp
    }
    val fontSize = when (size) {
        ProBadgeSize.COMPACT -> 9.sp
        ProBadgeSize.STANDARD -> 11.sp
    }
    val lineHeight = when (size) {
        ProBadgeSize.COMPACT -> 10.sp
        ProBadgeSize.STANDARD -> 12.sp
    }
    val fontWeight = when (size) {
        ProBadgeSize.COMPACT -> FontWeight.Bold
        ProBadgeSize.STANDARD -> FontWeight.ExtraBold
    }

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.tertiary)
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.badge_pro),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = fontSize,
                fontWeight = fontWeight,
                lineHeight = lineHeight
            ),
            color = MaterialTheme.colorScheme.onTertiary
        )
    }
}
