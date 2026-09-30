package es.pedrazamiguez.splittrip.core.designsystem.presentation.topbar

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
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
import dev.chrisbanes.haze.HazeState
import es.pedrazamiguez.splittrip.core.designsystem.R
import es.pedrazamiguez.splittrip.core.designsystem.extension.debouncedClickable
import es.pedrazamiguez.splittrip.core.designsystem.extension.debouncedCombinedClickable
import es.pedrazamiguez.splittrip.core.designsystem.foundation.GlassmorphismDefaults
import es.pedrazamiguez.splittrip.core.designsystem.foundation.LocalHazeState
import es.pedrazamiguez.splittrip.core.designsystem.foundation.horizonGlassEffect
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.ArrowLeft
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.shape.ExpressiveShapes
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.shape.RoundedPolygonShape

/**
 * A standard pinned TopAppBar with atmospheric gradient and glassmorphism styling.
 *
 * @param title The main title text
 * @param modifier The modifier to be applied to the top app bar container
 * @param subtitle Optional subtitle text
 * @param onBack Optional callback for back navigation. If null, no back button is shown.
 * @param onBackLongPress Optional callback for long-press back navigation (e.g., closing a wizard).
 * @param hazeState Optional [HazeState] for glassmorphism backdrop blur. Defaults to ambient [LocalHazeState].
 * @param actions Optional actions to display in the app bar
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DynamicTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    onBackLongPress: (() -> Unit)? = null,
    hazeState: HazeState? = LocalHazeState.current,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val isDark = isSystemInDarkTheme()
    val colors = resolveContentColors(isDark)

    val glassModifier = if (hazeState != null) {
        Modifier.horizonGlassEffect(hazeState = hazeState)
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(glassModifier)
            .background(GlassmorphismDefaults.topAppBarGradient(isDark))
    ) {
        TopAppBar(
            title = {
                DynamicTopAppBarTitle(
                    title = title,
                    subtitle = subtitle,
                    contentColor = colors.contentColor,
                    subtitleColor = colors.subtitleColor
                )
            },
            navigationIcon = {
                if (onBack != null) {
                    DynamicTopAppBarBackButton(
                        onBack = onBack,
                        onBackLongPress = onBackLongPress,
                        backgroundColor = colors.backButtonBg,
                        iconColor = colors.contentColor
                    )
                }
            },
            actions = {
                CompositionLocalProvider(LocalContentColor provides colors.contentColor) {
                    actions()
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                titleContentColor = colors.contentColor,
                navigationIconContentColor = colors.contentColor,
                actionIconContentColor = colors.contentColor
            )
        )
    }
}

private data class TopAppBarContentColors(
    val contentColor: Color,
    val subtitleColor: Color,
    val backButtonBg: Color
)

@Composable
private fun resolveContentColors(isDark: Boolean): TopAppBarContentColors {
    return if (isDark) {
        TopAppBarContentColors(
            contentColor = MaterialTheme.colorScheme.onSurface,
            subtitleColor = MaterialTheme.colorScheme.onSurfaceVariant,
            backButtonBg = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
        )
    } else {
        TopAppBarContentColors(
            contentColor = MaterialTheme.colorScheme.onPrimary,
            subtitleColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f),
            backButtonBg = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f)
        )
    }
}

@Composable
private fun DynamicTopAppBarTitle(
    title: String,
    subtitle: String?,
    contentColor: Color,
    subtitleColor: Color
) {
    Column {
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            color = contentColor
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = subtitleColor
            )
        }
    }
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
