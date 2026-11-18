package es.pedrazamiguez.splittrip.features.expense.presentation.component.category

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.constant.UiConstants
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import kotlinx.collections.immutable.ImmutableList

private const val DEFAULT_GRID_COLUMNS = 3
private const val SUBCATEGORY_FADE_IN_MS = 220
private const val SUBCATEGORY_FADE_IN_DELAY_MS = 60
private const val SUBCATEGORY_FADE_OUT_MS = 150

/**
 * Non-lazy multi-column grid selector rendering [CategoryGridTile] items in uniform rows.
 */
@Composable
internal fun <T> CategoryGridSelector(
    items: ImmutableList<T>,
    isItemSelected: (T) -> Boolean,
    onItemClicked: (T) -> Unit,
    itemId: (T) -> String,
    itemLabel: @Composable (T) -> String,
    itemIcon: (T) -> ImageVector?,
    modifier: Modifier = Modifier,
    columns: Int = DEFAULT_GRID_COLUMNS
) {
    if (items.isEmpty() || columns <= 0) return

    val isNarrowScreen = LocalConfiguration.current.screenWidthDp.dp <= UiConstants.NARROW_SCREEN_BREAKPOINT
    val horizontalSpacing =
        if (isNarrowScreen) MaterialTheme.spacing.ExtraSmall else MaterialTheme.spacing.Small
    val rows = items.chunked(columns)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium)
    ) {
        rows.forEach { rowItems ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(horizontalSpacing)
            ) {
                rowItems.forEach { item ->
                    key(itemId(item)) {
                        CategoryGridTile(
                            label = itemLabel(item),
                            icon = itemIcon(item),
                            selected = isItemSelected(item),
                            onClick = { onItemClicked(item) },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                }
                val emptySlots = columns - rowItems.size
                repeat(emptySlots) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

internal fun subcategoryGridEnterTransition(): EnterTransition =
    fadeIn(tween(SUBCATEGORY_FADE_IN_MS)) + expandVertically(
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        expandFrom = Alignment.Top
    )

internal fun subcategoryGridExitTransition(): ExitTransition =
    fadeOut(tween(SUBCATEGORY_FADE_OUT_MS)) + shrinkVertically(
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        shrinkTowards = Alignment.Top
    )

internal fun AnimatedContentTransitionScope<*>.subcategoryGridContentTransform(): ContentTransform =
    (
        fadeIn(tween(SUBCATEGORY_FADE_IN_MS, delayMillis = SUBCATEGORY_FADE_IN_DELAY_MS))
            togetherWith fadeOut(tween(SUBCATEGORY_FADE_OUT_MS))
        ).using(
        SizeTransform(clip = false) { _, _ ->
            spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        }
    )
