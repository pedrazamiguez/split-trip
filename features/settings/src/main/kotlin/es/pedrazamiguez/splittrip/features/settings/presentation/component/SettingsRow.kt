package es.pedrazamiguez.splittrip.features.settings.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.ChevronRight
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.LargeBodyText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.SecondaryBodyText
import es.pedrazamiguez.splittrip.features.settings.presentation.view.SettingItemView

@Composable
fun SettingsRow(
    item: SettingItemView,
    descriptionContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null
) {
    ListItem(
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent
        ),
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(22.dp)
                )
            }
        },
        headlineContent = {
            LargeBodyText(text = item.title)
        },
        supportingContent = {
            if (descriptionContent != null) {
                descriptionContent()
            } else {
                item.description?.let {
                    SecondaryBodyText(text = it, maxLines = Int.MAX_VALUE)
                }
            }
        },
        trailingContent = {
            if (trailingContent != null) {
                trailingContent()
            } else {
                Icon(
                    imageVector = TablerIcons.Outline.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        },
        modifier = Modifier
            .clickable { item.onClick() }
            .fillMaxWidth()
            .padding(horizontal = MaterialTheme.spacing.Small)
    )
}
