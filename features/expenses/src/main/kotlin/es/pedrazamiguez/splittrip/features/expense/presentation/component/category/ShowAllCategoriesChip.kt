package es.pedrazamiguez.splittrip.features.expense.presentation.component.category

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.extension.debounced
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Check
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.LayoutGrid
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.LabelText
import es.pedrazamiguez.splittrip.features.expense.R

private val ACTION_MIN_HEIGHT = 36.dp
private val ACTION_ICON_SIZE = 16.dp
private const val ACTIVE_CONTAINER_ALPHA = 0.12f
private const val COLOR_ANIMATION_MS = 180

@Composable
internal fun ShowAllCategoriesChip(
    showAllCategories: Boolean,
    onToggleShowAllCategories: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val containerColor by animateColorAsState(
        targetValue = if (showAllCategories) {
            colorScheme.primary.copy(alpha = ACTIVE_CONTAINER_ALPHA)
        } else {
            Color.Transparent
        },
        animationSpec = tween(COLOR_ANIMATION_MS),
        label = "ShowAllCategoriesContainerColor"
    )
    val debouncedToggle = debounced(onClick = onToggleShowAllCategories)

    TextButton(
        onClick = debouncedToggle,
        modifier = modifier
            .heightIn(min = ACTION_MIN_HEIGHT)
            .animateContentSize()
            .semantics { selected = showAllCategories },
        shape = CircleShape,
        colors = ButtonDefaults.textButtonColors(
            containerColor = containerColor,
            contentColor = colorScheme.primary
        ),
        contentPadding = PaddingValues(
            horizontal = MaterialTheme.spacing.Medium,
            vertical = MaterialTheme.spacing.ExtraSmall
        )
    ) {
        AnimatedContent(
            targetState = showAllCategories,
            transitionSpec = {
                (fadeIn(tween(COLOR_ANIMATION_MS)) togetherWith fadeOut(tween(COLOR_ANIMATION_MS)))
                    .using(SizeTransform(clip = false))
            },
            label = "ShowAllCategoriesIcon"
        ) { isExpanded ->
            Icon(
                imageVector = if (isExpanded) TablerIcons.Outline.Check else TablerIcons.Outline.LayoutGrid,
                contentDescription = null,
                modifier = Modifier.size(ACTION_ICON_SIZE)
            )
        }
        Spacer(modifier = Modifier.width(MaterialTheme.spacing.ExtraSmall))
        LabelText(
            text = stringResource(R.string.category_show_all_toggle),
            color = colorScheme.primary
        )
    }
}
