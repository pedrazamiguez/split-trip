package es.pedrazamiguez.splittrip.features.expense.presentation.component.category

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.extension.debouncedClickableNoRipple
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Check
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.LayoutGrid
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.LabelText
import es.pedrazamiguez.splittrip.features.expense.R

private val ACTION_MIN_HEIGHT = 36.dp
private val ACTION_ICON_SIZE = 16.dp
private const val ICON_ANIMATION_MS = 180

@Composable
internal fun ShowAllCategoriesChip(
    showAllCategories: Boolean,
    onToggleShowAllCategories: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary

    Row(
        modifier = modifier
            .heightIn(min = ACTION_MIN_HEIGHT)
            .debouncedClickableNoRipple(
                role = Role.Button,
                onClick = onToggleShowAllCategories
            )
            .semantics { selected = showAllCategories }
            .padding(
                start = MaterialTheme.spacing.Small,
                top = MaterialTheme.spacing.ExtraSmall,
                bottom = MaterialTheme.spacing.ExtraSmall
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimatedContent(
            targetState = showAllCategories,
            transitionSpec = {
                (fadeIn(tween(ICON_ANIMATION_MS)) togetherWith fadeOut(tween(ICON_ANIMATION_MS)))
                    .using(SizeTransform(clip = false))
            },
            label = "ShowAllCategoriesIcon"
        ) { isExpanded ->
            Icon(
                imageVector = if (isExpanded) TablerIcons.Outline.Check else TablerIcons.Outline.LayoutGrid,
                contentDescription = null,
                modifier = Modifier.size(ACTION_ICON_SIZE),
                tint = primaryColor
            )
        }
        Spacer(modifier = Modifier.width(MaterialTheme.spacing.ExtraSmall))
        LabelText(
            text = stringResource(R.string.category_show_all_toggle),
            color = primaryColor
        )
    }
}
