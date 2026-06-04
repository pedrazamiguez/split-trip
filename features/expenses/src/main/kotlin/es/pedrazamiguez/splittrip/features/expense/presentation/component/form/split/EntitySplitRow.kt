package es.pedrazamiguez.splittrip.features.expense.presentation.component.form.split

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.ChevronDown
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.ChevronUp
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Sitemap
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.input.StyledOutlinedTextField
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.SecondaryBodyText
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.model.SplitTypeUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.model.SplitUiModel
import kotlinx.collections.immutable.ImmutableList

@Suppress("LongMethod", "CyclomaticComplexMethod", "CognitiveComplexMethod")
@Composable
internal fun EntitySplitRow(
    entity: SplitUiModel,
    isEqualMode: Boolean,
    isPercentMode: Boolean,
    availableSplitTypes: ImmutableList<SplitTypeUiModel>,
    events: EntitySplitEditorEvents,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isSubunitHeader = entity.isEntityRow && entity.entityMembers.isNotEmpty()

    Column(modifier = modifier.fillMaxWidth()) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (isSubunitHeader) {
                        Modifier
                            .clip(MaterialTheme.shapes.large)
                            .clickable { events.onAccordionToggled(entity.userId) }
                    } else {
                        Modifier
                    }
                )
                .padding(vertical = MaterialTheme.spacing.ExtraSmall),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)
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

            // Name column
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
                if (!entity.isExcluded && !isEqualMode && entity.formattedAmount.isNotBlank()) {
                    SecondaryBodyText(
                        text = entity.formattedAmount,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = Int.MAX_VALUE
                    )
                }
            }

            // Input field
            AnimatedVisibility(visible = !entity.isExcluded) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.ExtraSmall)
                ) {
                    when {
                        isEqualMode -> Text(
                            text = entity.formattedAmount,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        isPercentMode -> {
                            StyledOutlinedTextField(
                                value = entity.percentageInput,
                                onValueChange = { events.onPercentageChanged(entity.userId, it) },
                                label = stringResource(R.string.add_expense_split_percentage_label),
                                modifier = Modifier.widthIn(max = 100.dp),
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Next,
                                keyboardActions = KeyboardActions(onNext = { onDone() })
                            )
                            ShareLockIcon(isLocked = entity.isShareLocked, onClick = {
                                events.onShareLockToggled(entity.userId)
                            })
                        }
                        else -> {
                            StyledOutlinedTextField(
                                value = entity.amountInput,
                                onValueChange = { events.onAmountChanged(entity.userId, it) },
                                label = stringResource(R.string.add_expense_split_amount_label),
                                modifier = Modifier.widthIn(max = 120.dp),
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Next,
                                keyboardActions = KeyboardActions(onNext = { onDone() })
                            )
                            ShareLockIcon(isLocked = entity.isShareLocked, onClick = {
                                events.onShareLockToggled(entity.userId)
                            })
                        }
                    }
                }
            }

            if (isSubunitHeader) {
                Icon(
                    imageVector = if (entity.isExpanded) {
                        TablerIcons.Outline.ChevronUp
                    } else {
                        TablerIcons.Outline.ChevronDown
                    },
                    contentDescription = stringResource(
                        if (entity.isExpanded) {
                            R.string.add_expense_split_subunit_collapse
                        } else {
                            R.string.add_expense_split_subunit_expand
                        }
                    ),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Switch(
                checked = !entity.isExcluded,
                onCheckedChange = { events.onExcludedToggled(entity.userId) }
            )
        }

        // Accordion content
        if (isSubunitHeader) {
            AnimatedVisibility(
                visible = entity.isExpanded && !entity.isExcluded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                IntraSubunitSplitEditor(
                    members = entity.entityMembers,
                    entitySplitType = entity.entitySplitType,
                    availableSplitTypes = availableSplitTypes,
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
                        .padding(
                            top = MaterialTheme.spacing.ExtraSmall,
                            bottom = MaterialTheme.spacing.ExtraSmall
                        )
                        .padding(MaterialTheme.spacing.Medium)
                )
            }
        }
    }
}
