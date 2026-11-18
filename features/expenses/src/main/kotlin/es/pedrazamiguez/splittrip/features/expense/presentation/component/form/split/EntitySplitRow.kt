package es.pedrazamiguez.splittrip.features.expense.presentation.component.form.split

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.constant.UiConstants
import es.pedrazamiguez.splittrip.core.designsystem.extension.debouncedClickableNoRipple
import es.pedrazamiguez.splittrip.core.designsystem.extension.debouncedCombinedClickableNoRipple
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.ChevronDown
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.ChevronUp
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Sitemap
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.FlatCard
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.SecondaryBodyText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.ShareAllocationInputRow
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.model.SplitTypeUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.model.SplitUiModel
import kotlinx.collections.immutable.ImmutableList

private val TRAILING_ICON_SLOT_SIZE = 24.dp
private const val DARK_THEME_LUMINANCE_THRESHOLD = 0.5f

@Suppress("LongMethod", "LongParameterList", "CyclomaticComplexMethod", "CognitiveComplexMethod")
@Composable
internal fun EntitySplitRow(
    entity: SplitUiModel,
    isEqualMode: Boolean,
    isPercentMode: Boolean,
    availableSplitTypes: ImmutableList<SplitTypeUiModel>,
    events: EntitySplitEditorEvents,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    currencySymbol: String? = null
) {
    val isSubunitHeader = entity.isEntityRow && entity.entityMembers.isNotEmpty()
    val isNarrowScreen = LocalConfiguration.current.screenWidthDp.dp <= UiConstants.NARROW_SCREEN_BREAKPOINT
    val rowGap = if (isNarrowScreen) MaterialTheme.spacing.ExtraSmall else MaterialTheme.spacing.Small
    val chevronIcon = if (entity.isExpanded) {
        TablerIcons.Outline.ChevronUp
    } else {
        TablerIcons.Outline.ChevronDown
    }
    val chevronDescription = stringResource(
        if (entity.isExpanded) {
            R.string.add_expense_split_subunit_collapse
        } else {
            R.string.add_expense_split_subunit_expand
        }
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = MaterialTheme.spacing.ExtraSmall),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (isSubunitHeader) {
                        Modifier
                            .clip(MaterialTheme.shapes.large)
                            .debouncedClickableNoRipple { events.onAccordionToggled(entity.userId) }
                    } else {
                        Modifier
                    }
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(rowGap)
        ) {
            if (isSubunitHeader) {
                Icon(
                    imageVector = TablerIcons.Outline.Sitemap,
                    contentDescription = null,
                    tint = if (entity.isExcluded) {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entity.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSubunitHeader) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (entity.isExcluded) {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                if (isSubunitHeader && !entity.isExcluded) {
                    SecondaryBodyText(
                        text = pluralStringResource(
                            R.plurals.add_expense_split_subunit_members_count,
                            entity.entityMembers.size,
                            entity.entityMembers.size
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = Int.MAX_VALUE
                    )
                }
            }

            if (!entity.isExcluded && entity.formattedAmount.isNotBlank()) {
                Text(
                    text = entity.formattedAmount,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isSubunitHeader) {
                Icon(
                    imageVector = chevronIcon,
                    contentDescription = chevronDescription,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (!entity.isExcluded && entity.formattedAmount.isNotBlank()) {
                // Match the 24.dp chevron slot so standalone prices align with subunit prices
                // and share the exact same distance to the toggle.
                Spacer(modifier = Modifier.size(TRAILING_ICON_SLOT_SIZE))
            }

            val haptic = LocalHapticFeedback.current
            Box(
                modifier = Modifier.debouncedCombinedClickableNoRipple(
                    onClick = { events.onExcludedToggled(entity.userId) },
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        events.onSoloSelected(entity.userId)
                    },
                    role = Role.Switch
                )
            ) {
                Switch(
                    checked = !entity.isExcluded,
                    onCheckedChange = null
                )
            }
        }

        AnimatedVisibility(visible = !isEqualMode && !entity.isExcluded) {
            val inputConfig = resolveSplitInputConfig(
                split = entity,
                isPercentMode = isPercentMode,
                currencySymbol = currencySymbol,
                onAmountChanged = { events.onAmountChanged(entity.userId, it) },
                onPercentageChanged = { events.onPercentageChanged(entity.userId, it) }
            )
            ShareAllocationInputRow(
                value = inputConfig.value,
                onValueChange = inputConfig.onValueChange,
                unitText = inputConfig.unitRes?.let { stringResource(it) } ?: inputConfig.unitLiteral,
                isLocked = entity.isShareLocked,
                lockContentDescription = stringResource(splitLockDescriptionRes(entity.isShareLocked)),
                onLockToggle = { events.onShareLockToggled(entity.userId) },
                fieldContentDescription = stringResource(inputConfig.fieldDescriptionRes),
                imeAction = ImeAction.Next,
                keyboardActions = KeyboardActions(onNext = { onDone() })
            )
        }

        if (isSubunitHeader) {
            AnimatedVisibility(
                visible = entity.isExpanded && !entity.isExcluded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                val isDark = MaterialTheme.colorScheme.surface.luminance() < DARK_THEME_LUMINANCE_THRESHOLD
                val expandedCardColor = if (isDark) {
                    MaterialTheme.colorScheme.surfaceContainerHighest
                } else {
                    MaterialTheme.colorScheme.surfaceContainerLowest
                }
                val cardPadding = if (isNarrowScreen) {
                    MaterialTheme.spacing.Medium
                } else {
                    MaterialTheme.spacing.Default
                }
                FlatCard(
                    modifier = Modifier.fillMaxWidth(),
                    color = expandedCardColor,
                    ghostBorder = true
                ) {
                    IntraSubunitSplitEditor(
                        members = entity.entityMembers,
                        entitySplitType = entity.entitySplitType,
                        availableSplitTypes = availableSplitTypes,
                        currencySymbol = currencySymbol,
                        onSplitTypeChanged = { splitTypeId ->
                            events.onIntraSubunitSplitTypeChanged(entity.userId, splitTypeId)
                        },
                        onAmountChanged = { userId, amount ->
                            events.onIntraSubunitAmountChanged(entity.userId, userId, amount)
                        },
                        onPercentageChanged = { userId, pct ->
                            events.onIntraSubunitPercentageChanged(entity.userId, userId, pct)
                        },
                        onShareLockToggled = { userId ->
                            events.onIntraSubunitShareLockToggled(entity.userId, userId)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(cardPadding)
                    )
                }
            }
        }
    }
}
