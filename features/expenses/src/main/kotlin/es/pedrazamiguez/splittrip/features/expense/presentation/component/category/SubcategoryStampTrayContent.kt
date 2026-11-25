package es.pedrazamiguez.splittrip.features.expense.presentation.component.category

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.constant.UiConstants
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip.PassportTagStyle
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.FlatCard
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun SubcategoryStampTrayContent(
    title: String,
    items: ImmutableList<SubcategoryStampItem>,
    onSubcategoryClicked: (String) -> Unit,
    modifier: Modifier = Modifier,
    badgeText: String? = null,
    badgeStyle: PassportTagStyle = PassportTagStyle.INFO,
    badgeContentDescription: String? = null,
    containedInCard: Boolean = true
) {
    val isNarrow = LocalConfiguration.current.screenWidthDp.dp <= UiConstants.NARROW_SCREEN_BREAKPOINT
    val contentPadding = if (isNarrow) MaterialTheme.spacing.Medium else MaterialTheme.spacing.Default
    val showBadge = !badgeText.isNullOrBlank()
    var retainedBadgeText by remember { mutableStateOf("") }
    var retainedBadgeDescription by remember { mutableStateOf<String?>(null) }
    if (!badgeText.isNullOrBlank()) {
        retainedBadgeText = badgeText
        retainedBadgeDescription = badgeContentDescription
    }
    val badgeModifier = retainedBadgeDescription?.let { desc ->
        Modifier.semantics { contentDescription = desc }
    } ?: Modifier
    val animatedModifier = modifier
        .fillMaxWidth()
        .animateContentSize(animationSpec = CategorySelectorTransitions.trayResizeSpec())

    if (containedInCard) {
        FlatCard(modifier = animatedModifier) {
            SubcategoryStampTrayBody(
                title = title,
                items = items,
                onSubcategoryClicked = onSubcategoryClicked,
                showBadge = showBadge,
                retainedBadgeText = retainedBadgeText,
                badgeStyle = badgeStyle,
                badgeModifier = badgeModifier,
                modifier = Modifier.padding(contentPadding)
            )
        }
    } else {
        SubcategoryStampTrayBody(
            title = title,
            items = items,
            onSubcategoryClicked = onSubcategoryClicked,
            showBadge = showBadge,
            retainedBadgeText = retainedBadgeText,
            badgeStyle = badgeStyle,
            badgeModifier = badgeModifier,
            modifier = animatedModifier
        )
    }
}
