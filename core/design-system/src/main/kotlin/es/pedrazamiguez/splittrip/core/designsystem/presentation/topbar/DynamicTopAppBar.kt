package es.pedrazamiguez.splittrip.core.designsystem.presentation.topbar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.R
import es.pedrazamiguez.splittrip.core.designsystem.extension.debouncedClickable
import es.pedrazamiguez.splittrip.core.designsystem.extension.debouncedCombinedClickable
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.ArrowLeft
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.shape.ExpressiveShapes
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.shape.RoundedPolygonShape

/**
 * A standard pinned TopAppBar with primary background and onPrimary content color.
 *
 * @param title The main title text
 * @param subtitle Optional subtitle text
 * @param onBack Optional callback for back navigation. If null, no back button is shown.
 * @param onBackLongPress Optional callback for long-press back navigation (e.g., closing a wizard).
 * @param actions Optional actions to display in the app bar
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DynamicTopAppBar(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    onBackLongPress: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                    )
                }
            }
        },
        navigationIcon = {
            if (onBack != null) {
                DynamicTopAppBarBackButton(
                    onBack = onBack,
                    onBackLongPress = onBackLongPress,
                    backgroundColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f),
                    iconColor = MaterialTheme.colorScheme.onPrimary
                )
            }
        },
        actions = {
            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onPrimary) {
                actions()
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary
        )
    )
}

@Composable
private fun DynamicTopAppBarBackButton(
    onBack: () -> Unit,
    onBackLongPress: (() -> Unit)?,
    backgroundColor: Color,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    val backButtonShape = remember {
        RoundedPolygonShape(ExpressiveShapes.softScallopedCircle())
    }
    val onClickLabel = stringResource(R.string.content_description_back)
    val clickModifier = if (onBackLongPress != null) {
        Modifier.debouncedCombinedClickable(
            onClick = onBack,
            onLongClick = onBackLongPress,
            onClickLabel = onClickLabel,
            role = Role.Button
        )
    } else {
        Modifier.debouncedClickable(
            onClick = onBack,
            onClickLabel = onClickLabel,
            role = Role.Button
        )
    }

    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .then(clickModifier)
            .size(40.dp)
            .clip(backButtonShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = TablerIcons.Outline.ArrowLeft,
            contentDescription = onClickLabel,
            tint = iconColor
        )
    }
}
