package es.pedrazamiguez.splittrip.features.expense.presentation.component.category

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.CaptionText
import es.pedrazamiguez.splittrip.features.expense.R

private val BADGE_MIN_SIZE = 20.dp
private val BADGE_HORIZONTAL_PADDING = 6.dp
private const val FOCUSED_BADGE_ALPHA = 0.2f

@Composable
internal fun CategoryCarouselCountBadge(
    count: Int,
    isFocused: Boolean,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val containerColor = if (isFocused) {
        colorScheme.onPrimary.copy(alpha = FOCUSED_BADGE_ALPHA)
    } else {
        colorScheme.primary
    }
    val contentColor = colorScheme.onPrimary
    val countDescription = pluralStringResource(R.plurals.category_filter_selected_count, count, count)

    Surface(
        shape = CircleShape,
        color = containerColor,
        contentColor = contentColor,
        modifier = modifier
            .defaultMinSize(minWidth = BADGE_MIN_SIZE, minHeight = BADGE_MIN_SIZE)
            .semantics { contentDescription = countDescription }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = BADGE_HORIZONTAL_PADDING)
        ) {
            CaptionText(
                text = count.toString(),
                color = contentColor
            )
        }
    }
}
