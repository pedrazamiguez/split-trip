package es.pedrazamiguez.splittrip.features.expense.presentation.component.category

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import es.pedrazamiguez.splittrip.core.designsystem.extension.debounced
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip.PassportChip
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip.PassportTagStyle
import kotlinx.collections.immutable.ImmutableList

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SubcategoryStampTrayContent(
    title: String,
    items: ImmutableList<SubcategoryStampItem>,
    onSubcategoryClicked: (String) -> Unit,
    modifier: Modifier = Modifier,
    badgeText: String? = null,
    badgeStyle: PassportTagStyle = PassportTagStyle.INFO,
    badgeContentDescription: String? = null,
    useSectionHeading: Boolean = false
) {
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

    Column(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = CategorySelectorTransitions.trayResizeSpec()),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium)
    ) {
        SubcategoryTrayHeaderRow(
            title = title,
            showBadge = showBadge,
            retainedBadgeText = retainedBadgeText,
            badgeStyle = badgeStyle,
            badgeModifier = badgeModifier,
            useSectionHeading = useSectionHeading
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)
        ) {
            items.forEach { item ->
                key(item.id) {
                    val debouncedClick = debounced { onSubcategoryClicked(item.id) }
                    PassportChip(
                        label = item.label,
                        selected = item.isSelected,
                        onClick = debouncedClick,
                        leadingIcon = filterChipLeadingIcon(
                            isSelected = item.isSelected,
                            unselectedIcon = item.icon
                        )
                    )
                }
            }
        }
    }
}
